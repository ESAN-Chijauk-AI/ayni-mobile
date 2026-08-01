package com.ayni.mobile.data.proximity

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.uwb.RangingParameters
import androidx.core.uwb.RangingResult
import androidx.core.uwb.UwbComplexChannel
import androidx.core.uwb.UwbControleeSessionScope
import androidx.core.uwb.UwbControllerSessionScope
import androidx.core.uwb.UwbDevice
import androidx.core.uwb.UwbManager
import com.ayni.mobile.domain.proximity.PeerConnectionState
import com.ayni.mobile.domain.proximity.GattConfirmationStatus
import com.ayni.mobile.domain.proximity.RangingTechnology
import com.ayni.mobile.domain.proximity.UwbRangingStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

private const val TAG = "AyniUwbRanging"

data class PreparedControllerSession(
    val handshake: UwbHandshake,
    val remoteAddress: ByteArray,
)

/**
 * Coordina UWB y deja BLE como fallback. Los parámetros efímeros se intercambian por
 * GATT; nunca se persisten ni salen del enlace local.
 */
@Singleton
class UwbRangingCoordinator @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val secureRandom = SecureRandom()
    private val mutableState = MutableStateFlow(PeerConnectionState())
    private var controleeScope: UwbControleeSessionScope? = null
    private var controllerScope: UwbControllerSessionScope? = null
    private var rangingJob: Job? = null

    val state: StateFlow<PeerConnectionState> = mutableState.asStateFlow()

    val hardwareSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            context.packageManager.hasSystemFeature("android.hardware.uwb")

    fun markGattConnecting(peerId: String) {
        reset(peerId)
        mutableState.value = mutableState.value.copy(
            confirmationStatus = GattConfirmationStatus.CONNECTING,
            errorMessage = null,
        )
    }

    fun markGattConfirmed(peerId: String) {
        mutableState.value = mutableState.value.copy(
            peerId = peerId,
            confirmationStatus = GattConfirmationStatus.CONFIRMED,
            errorMessage = null,
        )
    }

    fun markGattFailed(peerId: String, error: String? = null) {
        mutableState.value = mutableState.value.copy(
            peerId = peerId,
            confirmationStatus = GattConfirmationStatus.FAILED,
            technology = RangingTechnology.BLE_RSSI,
            errorMessage = error,
        )
    }

    fun markPeerHasNoUwb(peerId: String) {
        mutableState.value = mutableState.value.copy(
            peerId = peerId,
            uwbStatus = UwbRangingStatus.UNAVAILABLE,
            technology = RangingTechnology.BLE_RSSI,
        )
    }

    suspend fun prepareControlee(): ByteArray? {
        if (!hardwareSupported) return null
        return runCatching {
            val manager = UwbManager.createInstance(context)
            if (!manager.isAvailable()) return null
            manager.controleeSessionScope().also { controleeScope = it }.localAddress.address
        }.onFailure { Log.w(TAG, "UWB controlee no disponible", it) }
            .getOrNull()
    }

    suspend fun prepareController(peerId: String, remoteAddress: ByteArray): PreparedControllerSession? {
        if (!hardwareSupported) return null
        mutableState.value = mutableState.value.copy(
            peerId = peerId,
            uwbStatus = UwbRangingStatus.NEGOTIATING,
            errorMessage = null,
        )
        return runCatching {
            val manager = UwbManager.createInstance(context)
            if (!manager.isAvailable()) return null
            val sessionScope = manager.controllerSessionScope()
            controllerScope = sessionScope
            val channel = sessionScope.uwbComplexChannel
            val key = ByteArray(8).also(secureRandom::nextBytes)
            val sessionId = secureRandom.nextInt(Int.MAX_VALUE - 1) + 1
            PreparedControllerSession(
                handshake = UwbHandshake(
                    sessionId = sessionId,
                    sessionKey = key,
                    controllerAddress = sessionScope.localAddress.address,
                    channel = channel.channel,
                    preambleIndex = channel.preambleIndex,
                ),
                remoteAddress = remoteAddress,
            )
        }.onFailure {
            Log.w(TAG, "No se pudo preparar UWB controller", it)
            markUwbFailed(peerId, it)
        }.getOrNull()
    }

    fun startController(peerId: String, prepared: PreparedControllerSession) {
        val sessionScope = controllerScope ?: return markUwbFailed(peerId, null)
        startRanging(
            sessionScope = sessionScope,
            peerId = peerId,
            peerAddress = prepared.remoteAddress,
            handshake = prepared.handshake,
            complexChannel = null,
            publishMeasurements = true,
        )
    }

    fun startControlee(handshake: UwbHandshake) {
        val sessionScope = controleeScope ?: return
        startRanging(
            sessionScope = sessionScope,
            peerId = null,
            peerAddress = handshake.controllerAddress,
            handshake = handshake,
            complexChannel = UwbComplexChannel(handshake.channel, handshake.preambleIndex),
            publishMeasurements = false,
        )
    }

    fun reset(peerId: String? = null) {
        rangingJob?.cancel()
        rangingJob = null
        controleeScope = null
        controllerScope = null
        mutableState.value = PeerConnectionState(
            peerId = peerId,
            uwbStatus = if (hardwareSupported) UwbRangingStatus.READY else UwbRangingStatus.UNAVAILABLE,
        )
    }

    private fun startRanging(
        sessionScope: androidx.core.uwb.UwbClientSessionScope,
        peerId: String?,
        peerAddress: ByteArray,
        handshake: UwbHandshake,
        complexChannel: UwbComplexChannel?,
        publishMeasurements: Boolean,
    ) {
        rangingJob?.cancel()
        val parameters = RangingParameters(
            uwbConfigType = RangingParameters.CONFIG_UNICAST_DS_TWR,
            sessionId = handshake.sessionId,
            subSessionId = 0,
            sessionKeyInfo = handshake.sessionKey,
            subSessionKeyInfo = null,
            complexChannel = complexChannel,
            peerDevices = listOf(UwbDevice.createForAddress(peerAddress)),
            updateRateType = RangingParameters.RANGING_UPDATE_RATE_FREQUENT,
        )
        rangingJob = scope.launch {
            sessionScope.prepareSession(parameters)
                .catch {
                    Log.w(TAG, "Sesión UWB interrumpida", it)
                    if (publishMeasurements) markUwbFailed(peerId, it)
                }
                .collect { result ->
                    if (!publishMeasurements) return@collect
                    when (result) {
                        is RangingResult.RangingResultInitialized -> {
                            mutableState.value = mutableState.value.copy(
                                peerId = peerId,
                                uwbStatus = UwbRangingStatus.RANGING,
                                technology = RangingTechnology.UWB,
                                errorMessage = null,
                            )
                        }
                        is RangingResult.RangingResultPosition -> {
                            mutableState.value = mutableState.value.copy(
                                peerId = peerId,
                                uwbStatus = UwbRangingStatus.RANGING,
                                technology = RangingTechnology.UWB,
                                distanceMeters = result.position.distance?.value,
                                azimuthDegrees = result.position.azimuth?.value,
                                elevationDegrees = result.position.elevation?.value,
                                errorMessage = null,
                            )
                        }
                        is RangingResult.RangingResultFailure,
                        is RangingResult.RangingResultPeerDisconnected -> markUwbFailed(peerId, null)
                    }
                }
        }
    }

    private fun markUwbFailed(peerId: String?, error: Throwable?) {
        mutableState.value = mutableState.value.copy(
            peerId = peerId,
            uwbStatus = UwbRangingStatus.FAILED,
            technology = RangingTechnology.BLE_RSSI,
            distanceMeters = null,
            azimuthDegrees = null,
            elevationDegrees = null,
            errorMessage = error?.localizedMessage,
        )
    }
}
