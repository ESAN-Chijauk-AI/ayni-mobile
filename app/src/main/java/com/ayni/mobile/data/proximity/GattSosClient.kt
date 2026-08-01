package com.ayni.mobile.data.proximity

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

private const val TAG = "AyniGattSosClient"

@Singleton
class GattSosClient @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val uwb: UwbRangingCoordinator,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var gatt: BluetoothGatt? = null
    private var peerId: String? = null
    private var remoteUwbAddress: ByteArray? = null
    private var preparedUwb: PreparedControllerSession? = null
    private var capabilityReadAttempts = 0

    private val callback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val currentPeer = peerId ?: return
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "GATT conectado con $currentPeer")
                if (!gatt.discoverServices()) fail(currentPeer, "No se pudieron descubrir servicios")
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (uwb.state.value.confirmationStatus != com.ayni.mobile.domain.proximity.GattConfirmationStatus.CONFIRMED) {
                    fail(currentPeer, "La conexión GATT se cerró antes de confirmar")
                }
                runCatching { gatt.close() }
                if (this@GattSosClient.gatt === gatt) this@GattSosClient.gatt = null
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val currentPeer = peerId ?: return
            if (status != BluetoothGatt.GATT_SUCCESS) return fail(currentPeer, "Error al descubrir GATT")
            val characteristic = gatt.getService(ProximityProtocol.SERVICE_UUID)
                ?.getCharacteristic(ProximityProtocol.CAPABILITIES_UUID)
                ?: return fail(currentPeer, "El SOS no expone el protocolo de confirmación")
            if (!gatt.readCharacteristic(characteristic)) fail(currentPeer, "No se pudo leer compatibilidad")
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int,
        ) {
            handleCharacteristicRead(gatt, characteristic, value, status)
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                handleCharacteristicRead(gatt, characteristic, characteristic.value ?: byteArrayOf(), status)
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            val currentPeer = peerId ?: return
            if (status != BluetoothGatt.GATT_SUCCESS) return fail(currentPeer, "El SOS no confirmó la escritura GATT")
            when (characteristic.value?.getOrNull(1)) {
                ProximityProtocol.COMMAND_CONFIRM -> {
                    uwb.markGattConfirmed(currentPeer)
                    Log.i(TAG, "SOS $currentPeer confirmado por GATT")
                    val remoteAddress = remoteUwbAddress
                    if (remoteAddress == null || !uwb.hardwareSupported) {
                        uwb.markPeerHasNoUwb(currentPeer)
                        return
                    }
                    scope.launch {
                        val prepared = uwb.prepareController(currentPeer, remoteAddress)
                        if (prepared == null) {
                            uwb.markPeerHasNoUwb(currentPeer)
                            return@launch
                        }
                        preparedUwb = prepared
                        val control = gatt.getService(ProximityProtocol.SERVICE_UUID)
                            ?.getCharacteristic(ProximityProtocol.CONTROL_UUID)
                        if (control == null || !write(gatt, control, ProximityProtocol.encodeUwbHandshake(prepared.handshake))) {
                            uwb.markPeerHasNoUwb(currentPeer)
                        }
                    }
                }
                ProximityProtocol.COMMAND_START_UWB -> {
                    preparedUwb?.let { uwb.startController(currentPeer, it) }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(peerId: String, device: BluetoothDevice) {
        disconnect()
        this.peerId = peerId
        capabilityReadAttempts = 0
        uwb.markGattConnecting(peerId)
        gatt = runCatching {
            device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
        }.onFailure { fail(peerId, it.localizedMessage) }.getOrNull()
        if (gatt == null) fail(peerId, "No se pudo abrir la conexión GATT")
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        val active = gatt
        gatt = null
        peerId = null
        remoteUwbAddress = null
        preparedUwb = null
        capabilityReadAttempts = 0
        runCatching { active?.disconnect() }
        runCatching { active?.close() }
        uwb.reset()
    }

    @SuppressLint("MissingPermission")
    private fun handleCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int,
    ) {
        val currentPeer = peerId ?: return
        if (characteristic.uuid != ProximityProtocol.CAPABILITIES_UUID || status != BluetoothGatt.GATT_SUCCESS) {
            return fail(currentPeer, "No se pudieron leer capacidades GATT")
        }
        val capabilities = ProximityProtocol.decodeCapabilities(value)
            ?: return fail(currentPeer, "Versión de protocolo SOS incompatible")
        remoteUwbAddress = capabilities.uwbAddress
        if (capabilities.uwbAddress == null && uwb.hardwareSupported && capabilityReadAttempts < 2) {
            capabilityReadAttempts++
            scope.launch {
                delay(600)
                if (!gatt.readCharacteristic(characteristic)) {
                    fail(currentPeer, "No se pudo reintentar la negociación UWB")
                }
            }
            return
        }
        val control = gatt.getService(ProximityProtocol.SERVICE_UUID)
            ?.getCharacteristic(ProximityProtocol.CONTROL_UUID)
            ?: return fail(currentPeer, "Falta el canal GATT de control")
        if (!write(gatt, control, byteArrayOf(ProximityProtocol.VERSION, ProximityProtocol.COMMAND_CONFIRM))) {
            fail(currentPeer, "No se pudo enviar confirmación GATT")
        }
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun write(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray): Boolean {
        characteristic.value = value // Conserva el comando para identificar el callback en todas las APIs.
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(
                characteristic,
                value,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
            ) == BluetoothGatt.GATT_SUCCESS
        } else {
            gatt.writeCharacteristic(characteristic)
        }
    }

    private fun fail(peerId: String, message: String?) {
        Log.w(TAG, "Fallo GATT para $peerId: $message")
        uwb.markGattFailed(peerId, message)
    }
}
