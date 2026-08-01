package com.ayni.mobile.data.proximity

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.ayni.mobile.domain.proximity.SosModeStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AyniSosAdvertiser"

@Singleton
class BleSosAdvertiser @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val stateStore: SosStateStore,
) {
    private var callback: AdvertiseCallback? = null

    @SuppressLint("MissingPermission")
    fun start() {
        if (callback != null) return
        stateStore.update(SosModeStatus.STARTING)

        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
        val advertiser = runCatching { adapter?.bluetoothLeAdvertiser }.getOrNull()
        if (adapter == null || advertiser == null) {
            Log.e(TAG, "BLE advertising no está soportado por este teléfono")
            stateStore.update(SosModeStatus.UNSUPPORTED)
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            // Conectable para que el detector entregue un acuse verificable por GATT.
            .setConnectable(true)
            .setTimeout(0)
            .build()
        // El detector filtra exactamente este mismo campo Service UUID. Mantener el
        // paquete pequeño mejora compatibilidad con advertising legacy entre OEMs.
        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(ProximityProtocol.SERVICE_UUID))
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .build()

        val advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                Log.i(TAG, "SOS advertising iniciado; UUID=${ProximityProtocol.SERVICE_UUID}")
                stateStore.update(SosModeStatus.ACTIVE)
            }

            override fun onStartFailure(errorCode: Int) {
                Log.e(TAG, "SOS advertising falló; errorCode=$errorCode")
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
        Log.i(TAG, "Solicitando inicio de SOS advertising")
        runCatching { advertiser.startAdvertising(settings, data, advertiseCallback) }
            .onFailure {
                Log.e(TAG, "Excepción al iniciar SOS advertising", it)
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
        Log.i(TAG, "SOS advertising detenido")
        stateStore.update(SosModeStatus.INACTIVE)
    }
}
