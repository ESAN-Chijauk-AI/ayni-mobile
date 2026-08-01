package com.ayni.mobile.data.proximity

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "AyniGattSosServer"

@Singleton
class GattSosServer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val stateStore: SosStateStore,
    private val uwb: UwbRangingCoordinator,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var server: BluetoothGattServer? = null
    @Volatile private var uwbAddress: ByteArray? = null

    private val callback = object : BluetoothGattServerCallback() {
        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic,
        ) {
            if (characteristic.uuid != ProximityProtocol.CAPABILITIES_UUID) {
                respond(device, requestId, BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED, offset, null)
                return
            }
            val value = ProximityProtocol.encodeCapabilities(uwbAddress)
            if (offset !in 0..value.size) {
                respond(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset, null)
            } else {
                respond(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value.copyOfRange(offset, value.size))
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray,
        ) {
            var status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED
            if (characteristic.uuid == ProximityProtocol.CONTROL_UUID && !preparedWrite && offset == 0) {
                status = when {
                    value.contentEquals(byteArrayOf(ProximityProtocol.VERSION, ProximityProtocol.COMMAND_CONFIRM)) -> {
                        stateStore.confirmDetector(device.address)
                        Log.i(TAG, "Confirmación GATT recibida de ${device.address}")
                        BluetoothGatt.GATT_SUCCESS
                    }
                    value.getOrNull(1) == ProximityProtocol.COMMAND_START_UWB -> {
                        ProximityProtocol.decodeUwbHandshake(value)?.let {
                            uwb.startControlee(it)
                            BluetoothGatt.GATT_SUCCESS
                        } ?: BluetoothGatt.GATT_FAILURE
                    }
                    else -> BluetoothGatt.GATT_FAILURE
                }
            }
            if (responseNeeded) respond(device, requestId, status, offset, null)
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (server != null) return
        uwb.reset()
        uwbAddress = null
        stateStore.resetSession(uwbAvailable = false)
        val manager = context.getSystemService(BluetoothManager::class.java) ?: return
        val opened = runCatching { manager.openGattServer(context, callback) }.getOrNull()
        if (opened == null) {
            Log.e(TAG, "No se pudo abrir el servidor GATT")
            return
        }

        val service = BluetoothGattService(
            ProximityProtocol.SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY,
        )
        service.addCharacteristic(
            BluetoothGattCharacteristic(
                ProximityProtocol.CAPABILITIES_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ,
            ),
        )
        service.addCharacteristic(
            BluetoothGattCharacteristic(
                ProximityProtocol.CONTROL_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE,
            ),
        )
        server = opened
        if (!opened.addService(service)) {
            Log.e(TAG, "No se pudo publicar el servicio GATT SOS")
            stop()
            return
        }

        scope.launch {
            val address = uwb.prepareControlee()
            uwbAddress = address
            stateStore.setUwbAvailable(address != null)
            Log.i(TAG, if (address != null) "UWB listo para negociación" else "SOS operando con BLE/GATT")
        }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        val active = server
        server = null
        uwbAddress = null
        runCatching { active?.clearServices() }
        runCatching { active?.close() }
        uwb.reset()
    }

    @SuppressLint("MissingPermission")
    private fun respond(
        device: BluetoothDevice,
        requestId: Int,
        status: Int,
        offset: Int,
        value: ByteArray?,
    ) {
        runCatching { server?.sendResponse(device, requestId, status, offset, value) }
    }
}
