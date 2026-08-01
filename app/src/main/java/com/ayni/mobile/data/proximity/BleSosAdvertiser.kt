package com.ayni.mobile.data.proximity

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import com.ayni.mobile.domain.proximity.SosModeStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleSosAdvertiser @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val stateStore: SosStateStore,
) {
    private val random = SecureRandom()
    private var callback: AdvertiseCallback? = null

    @SuppressLint("MissingPermission")
    fun start() {
        if (callback != null) return
        stateStore.update(SosModeStatus.STARTING)

        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
        val advertiser = runCatching { adapter?.bluetoothLeAdvertiser }.getOrNull()
        if (adapter == null || advertiser == null) {
            stateStore.update(SosModeStatus.UNSUPPORTED)
            return
        }

        val peerId = ByteArray(ProximityProtocol.PEER_ID_SIZE).also(random::nextBytes)
        val payload = byteArrayOf(
            ProximityProtocol.PROTOCOL_VERSION,
            ProximityProtocol.MESSAGE_TYPE_SOS,
        ) + peerId

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .setTimeout(0)
            .build()
        // Service Data mantiene UUID + payload dentro del límite legacy de 31 bytes.
        val data = AdvertiseData.Builder()
            .addServiceData(ParcelUuid(ProximityProtocol.SERVICE_UUID), payload)
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .build()

        val advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                stateStore.update(SosModeStatus.ACTIVE)
            }

            override fun onStartFailure(errorCode: Int) {
                callback = null
                stateStore.update(
                    if (errorCode == ADVERTISE_FAILED_FEATURE_UNSUPPORTED) {
                        SosModeStatus.UNSUPPORTED
                    } else {
                        SosModeStatus.ERROR
                    },
                )
            }
        }
        callback = advertiseCallback
        runCatching { advertiser.startAdvertising(settings, data, advertiseCallback) }
            .onFailure {
                callback = null
                stateStore.update(SosModeStatus.ERROR)
            }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        val activeCallback = callback
        callback = null
        if (activeCallback != null) {
            val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
            runCatching { adapter?.bluetoothLeAdvertiser?.stopAdvertising(activeCallback) }
        }
        stateStore.update(SosModeStatus.INACTIVE)
    }
}
