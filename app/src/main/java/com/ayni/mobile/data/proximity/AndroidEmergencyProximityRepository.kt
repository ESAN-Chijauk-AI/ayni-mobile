package com.ayni.mobile.data.proximity

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.ayni.mobile.domain.proximity.EmergencyProximityRepository
import com.ayni.mobile.domain.proximity.NearbySosSignal
import com.ayni.mobile.domain.proximity.ProximityScanStatus
import com.ayni.mobile.domain.proximity.SosModeStatus
import com.ayni.mobile.domain.proximity.SosReceptionState
import com.ayni.mobile.domain.proximity.PeerConnectionState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidEmergencyProximityRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val scanner: BleSosScanner,
    private val sosStateStore: SosStateStore,
    private val gattClient: GattSosClient,
    private val uwb: UwbRangingCoordinator,
) : EmergencyProximityRepository {

    override val sosStatus: StateFlow<SosModeStatus> = sosStateStore.status
    override val scanStatus: StateFlow<ProximityScanStatus> = scanner.status
    override val nearbySignals: StateFlow<List<NearbySosSignal>> = scanner.signals
    override val sosReceptionState: StateFlow<SosReceptionState> = sosStateStore.receptionState
    override val peerConnectionState: StateFlow<PeerConnectionState> = uwb.state

    override fun activateSos() {
        scanner.stop()
        gattClient.disconnect()
        sosStateStore.update(SosModeStatus.STARTING)
        val intent = Intent(context, EmergencyProximityService::class.java)
            .setAction(EmergencyProximityService.ACTION_START)
        runCatching { ContextCompat.startForegroundService(context, intent) }
            .onFailure { sosStateStore.update(SosModeStatus.ERROR) }
    }

    override fun deactivateSos() {
        context.stopService(Intent(context, EmergencyProximityService::class.java))
        sosStateStore.update(SosModeStatus.INACTIVE)
    }

    override fun startDetection() {
        if (sosStateStore.status.value == SosModeStatus.INACTIVE) scanner.start()
    }

    override fun stopDetection() {
        gattClient.disconnect()
        scanner.stop()
    }

    override fun confirmAndRange(peerId: String) {
        val device = scanner.deviceFor(peerId)
        if (device == null) {
            uwb.markGattFailed(peerId, "La señal dejó de estar disponible")
            return
        }
        gattClient.connect(peerId, device)
    }

    override fun disconnectPeer() {
        gattClient.disconnect()
    }
}
