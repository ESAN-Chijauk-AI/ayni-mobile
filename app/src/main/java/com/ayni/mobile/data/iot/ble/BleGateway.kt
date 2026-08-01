package com.ayni.mobile.data.iot.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import com.ayni.mobile.data.iot.device.SensorNodeClient
import com.ayni.mobile.domain.iot.DiscoveredNode
import com.ayni.mobile.domain.iot.LinkState
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicLong

@SuppressLint("MissingPermission")
class BleGateway(
    context: Context,
    private val listener: SensorNodeClient.Listener,
) : SensorNodeClient {

    private sealed interface GattOperation {
        data class WriteDescriptor(
            val descriptor: BluetoothGattDescriptor,
            val value: ByteArray,
        ) : GattOperation

        data class WriteCharacteristic(
            val characteristic: BluetoothGattCharacteristic,
            val value: ByteArray,
        ) : GattOperation

        data class ReadCharacteristic(
            val characteristic: BluetoothGattCharacteristic,
        ) : GattOperation
    }

    private val applicationContext = context.applicationContext
    private val bluetoothManager =
        applicationContext.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter
    private val mainHandler = Handler(Looper.getMainLooper())
    private val assembler = NotificationAssembler()
    private val commandIds = AtomicLong((System.currentTimeMillis() and 0x7fffffffL).coerceAtLeast(1))
    private val nodesByAddress = linkedMapOf<String, DiscoveredNode>()
    private val operations = ArrayDeque<GattOperation>()

    private var currentGatt: BluetoothGatt? = null
    private var commandCharacteristic: BluetoothGattCharacteristic? = null
    private var operationInFlight = false
    private var discoveringServices = false
    private var scanning = false
    private var ready = false

    private val stopScanRunnable = Runnable { stopScan(timedOut = true) }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val advertisedName = result.scanRecord?.deviceName
            val name = advertisedName
                ?: runCatching { result.device.name }.getOrNull()
                ?: "Nodo SISMO"
            nodesByAddress[result.device.address] = DiscoveredNode(
                name = name,
                address = result.device.address,
                rssi = result.rssi,
            )
            listener.onNodesChanged(nodesByAddress.values.sortedByDescending { it.rssi })
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
        }

        override fun onScanFailed(errorCode: Int) {
            scanning = false
            mainHandler.removeCallbacks(stopScanRunnable)
            listener.onLinkState(LinkState.ERROR, "Falló el escaneo BLE ($errorCode)")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (gatt !== currentGatt) {
                gatt.close()
                return
            }
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        failConnection("Conexión GATT rechazada ($status)")
                        return
                    }
                    listener.onLinkState(LinkState.DISCOVERING, "Negociando enlace…")
                    gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                    discoveringServices = false
                    if (!gatt.requestMtu(DESIRED_MTU)) discoverServices(gatt)
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    val reason = if (status == BluetoothGatt.GATT_SUCCESS) {
                        "Sensor desconectado"
                    } else {
                        "Enlace perdido ($status)"
                    }
                    closeGatt(gatt)
                    listener.onLinkState(LinkState.DISCONNECTED, reason)
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (gatt === currentGatt) discoverServices(gatt)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (gatt !== currentGatt) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                failConnection("No se pudieron descubrir los servicios ($status)")
                return
            }
            val service = gatt.getService(BleProtocol.SERVICE_UUID)
            if (service == null) {
                failConnection("El dispositivo no publica el servicio SISMO")
                return
            }
            val statusCharacteristic = service.getCharacteristic(BleProtocol.STATUS_UUID)
            val eventCharacteristic = service.getCharacteristic(BleProtocol.EVENT_UUID)
            val stateCharacteristic =
                service.getCharacteristic(BleProtocol.STRUCTURAL_STATE_UUID)
            commandCharacteristic = service.getCharacteristic(BleProtocol.COMMAND_UUID)
            val protocolCharacteristic = service.getCharacteristic(BleProtocol.PROTOCOL_UUID)
            val traceCharacteristic = service.getCharacteristic(BleProtocol.TRACE_UUID)

            if (statusCharacteristic == null || eventCharacteristic == null ||
                stateCharacteristic == null || commandCharacteristic == null ||
                protocolCharacteristic == null || traceCharacteristic == null
            ) {
                failConnection("El servicio BLE está incompleto o usa otra versión")
                return
            }

            operations.clear()
            operationInFlight = false
            subscribe(gatt, statusCharacteristic)
            subscribe(gatt, eventCharacteristic)
            subscribe(gatt, stateCharacteristic)
            subscribe(gatt, traceCharacteristic)
            enqueue(GattOperation.ReadCharacteristic(protocolCharacteristic))
            ready = true
            listener.onLinkState(LinkState.READY, "Sensor conectado")
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            finishOperation(gatt, status, "suscripción")
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            finishOperation(gatt, status, "comando")
        }

        // API 33+ entrega el valor como parámetro; en API 26–32 llega el callback
        // legacy y hay que leer characteristic.value. Se sobrescriben ambos.
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
            handleCharacteristicRead(
                gatt,
                characteristic,
                characteristic.value ?: ByteArray(0),
                status,
            )
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            handleCharacteristicChanged(gatt, characteristic, value)
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            handleCharacteristicChanged(
                gatt,
                characteristic,
                characteristic.value ?: ByteArray(0),
            )
        }
    }

    private fun handleCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int,
    ) {
        if (status == BluetoothGatt.GATT_SUCCESS &&
            characteristic.uuid == BleProtocol.PROTOCOL_UUID
        ) {
            listener.onProtocolDescription(value.decodeToString())
        }
        finishOperation(gatt, status, "lectura")
    }

    private fun handleCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
    ) {
        if (gatt !== currentGatt) return
        runCatching { assembler.accept(characteristic.uuid, value) }
            .onFailure { listener.onError(it.message ?: "Notificación BLE inválida") }
            .getOrNull()
            ?.let { dispatch(characteristic.uuid, it) }
    }

    override fun startScan() {
        val bluetoothAdapter = adapter
        if (bluetoothAdapter == null) {
            listener.onLinkState(LinkState.ERROR, "Este teléfono no tiene Bluetooth")
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            listener.onLinkState(LinkState.ERROR, "Activa Bluetooth para buscar sensores")
            return
        }
        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            listener.onLinkState(LinkState.ERROR, "El escáner BLE no está disponible")
            return
        }

        stopScan(timedOut = false)
        nodesByAddress.clear()
        listener.onNodesChanged(emptyList())
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BleProtocol.SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()
        scanning = true
        listener.onLinkState(LinkState.SCANNING, "Buscando nodos cercanos…")
        scanner.startScan(listOf(filter), settings, scanCallback)
        mainHandler.postDelayed(stopScanRunnable, SCAN_DURATION_MS)
    }

    override fun connect(address: String) {
        val bluetoothAdapter = adapter
        if (bluetoothAdapter?.isEnabled != true) {
            listener.onLinkState(LinkState.ERROR, "Bluetooth está apagado")
            return
        }
        stopScan(timedOut = false)
        currentGatt?.let { closeGatt(it) }
        listener.onLinkState(LinkState.CONNECTING, "Conectando con el sensor…")
        currentGatt = bluetoothAdapter.getRemoteDevice(address).connectGatt(
            applicationContext,
            false,
            gattCallback,
            android.bluetooth.BluetoothDevice.TRANSPORT_LE,
        )
    }

    override fun disconnect() {
        stopScan(timedOut = false)
        val gatt = currentGatt
        if (gatt == null) {
            listener.onLinkState(LinkState.IDLE, "Sin conexión")
        } else {
            ready = false
            gatt.disconnect()
        }
    }

    override fun sendCommand(name: String, argument: Long?): Long? =
        write(name) { id -> BleProtocol.command(id, name, argument) }

    override fun sendTextCommand(name: String, argument: String): Long? =
        write(name) { id -> BleProtocol.command(id, name, argument) }

    private fun write(name: String, encode: (Long) -> ByteArray): Long? {
        val characteristic = commandCharacteristic
        if (!ready || characteristic == null) {
            listener.onError("Conecta un sensor antes de enviar comandos")
            return null
        }
        var id = commandIds.incrementAndGet() and 0xffffffffL
        if (id == 0L) id = commandIds.incrementAndGet() and 0xffffffffL
        val value = encode(id)
        if (value.size > BleProtocol.COMMAND_MAX_BYTES) {
            // El nodo trunca en silencio lo que exceda su característica, y un
            // comando a medias es peor que uno no enviado.
            listener.onError("El comando $name no cabe en el enlace BLE")
            return null
        }
        enqueue(
            GattOperation.WriteCharacteristic(
                characteristic = characteristic,
                value = value,
            ),
        )
        return id
    }

    override fun close() {
        mainHandler.removeCallbacks(stopScanRunnable)
        stopScan(timedOut = false)
        currentGatt?.let { closeGatt(it) }
    }

    private fun stopScan(timedOut: Boolean) {
        mainHandler.removeCallbacks(stopScanRunnable)
        if (!scanning) return
        scanning = false
        runCatching { adapter?.bluetoothLeScanner?.stopScan(scanCallback) }
        if (timedOut) {
            val message = if (nodesByAddress.isEmpty()) {
                "No se encontraron nodos. Acércate y vuelve a buscar."
            } else {
                "Búsqueda terminada"
            }
            listener.onLinkState(LinkState.IDLE, message)
        }
    }

    private fun discoverServices(gatt: BluetoothGatt) {
        if (discoveringServices) return
        discoveringServices = true
        if (!gatt.discoverServices()) failConnection("No se pudo iniciar el descubrimiento GATT")
    }

    private fun subscribe(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
    ) {
        if (!gatt.setCharacteristicNotification(characteristic, true)) {
            listener.onError("No se pudo habilitar ${characteristic.uuid}")
            return
        }
        val descriptor = characteristic.getDescriptor(BleProtocol.CCCD_UUID)
        if (descriptor == null) {
            listener.onError("Característica sin descriptor de notificación")
            return
        }
        enqueue(
            GattOperation.WriteDescriptor(
                descriptor = descriptor,
                value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE,
            ),
        )
    }

    @Synchronized
    private fun enqueue(operation: GattOperation) {
        operations.addLast(operation)
        startNextOperation()
    }

    @Synchronized
    private fun startNextOperation() {
        if (operationInFlight) return
        val gatt = currentGatt ?: return
        while (operations.isNotEmpty()) {
            val operation = operations.removeFirst()
            val started = when (operation) {
                is GattOperation.WriteDescriptor ->
                    writeDescriptorCompat(gatt, operation.descriptor, operation.value)

                is GattOperation.WriteCharacteristic ->
                    writeCharacteristicCompat(gatt, operation.characteristic, operation.value)

                is GattOperation.ReadCharacteristic ->
                    gatt.readCharacteristic(operation.characteristic)
            }
            if (started) {
                operationInFlight = true
                return
            }
            listener.onError("Android rechazó una operación GATT")
        }
    }

    // API 33+ tiene sobrecargas que reciben el valor por parámetro y devuelven un código;
    // en API 26–32 hay que fijar el valor en el objeto y usar el retorno booleano.
    @Suppress("DEPRECATION")
    private fun writeCharacteristicCompat(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
    ): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(
                characteristic,
                value,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
            ) == BluetoothStatusCodes.SUCCESS
        } else {
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            characteristic.value = value
            gatt.writeCharacteristic(characteristic)
        }

    @Suppress("DEPRECATION")
    private fun writeDescriptorCompat(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        value: ByteArray,
    ): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(descriptor, value) == BluetoothStatusCodes.SUCCESS
        } else {
            descriptor.value = value
            gatt.writeDescriptor(descriptor)
        }

    @Synchronized
    private fun finishOperation(gatt: BluetoothGatt, status: Int, label: String) {
        if (gatt !== currentGatt) return
        operationInFlight = false
        if (status != BluetoothGatt.GATT_SUCCESS) {
            listener.onError("Falló la $label GATT ($status)")
        }
        startNextOperation()
    }

    private fun dispatch(characteristic: java.util.UUID, json: String) {
        runCatching {
            when (characteristic) {
                BleProtocol.STATUS_UUID ->
                    listener.onOperationalStatus(BleJsonParser.operationalStatus(json))

                BleProtocol.STRUCTURAL_STATE_UUID ->
                    listener.onStructuralSnapshot(BleJsonParser.structuralSnapshot(json))

                BleProtocol.TRACE_UUID ->
                    listener.onMeasurementTrace(BleJsonParser.measurementTrace(json))

                BleProtocol.EVENT_UUID ->
                    if (BleJsonParser.eventName(json) == "seismic_event_completed") {
                        listener.onSeismicEvent(BleJsonParser.seismicEvent(json))
                    } else {
                        listener.onProtocolEvent(BleJsonParser.event(json))
                    }
            }
        }.onFailure {
            listener.onError("No se pudo interpretar BLE: ${it.message}")
        }
    }

    private fun failConnection(message: String) {
        listener.onLinkState(LinkState.ERROR, message)
        currentGatt?.disconnect()
    }

    private fun closeGatt(gatt: BluetoothGatt) {
        if (gatt === currentGatt) currentGatt = null
        ready = false
        discoveringServices = false
        commandCharacteristic = null
        operations.clear()
        operationInFlight = false
        assembler.clear()
        gatt.close()
    }

    private companion object {
        const val DESIRED_MTU = 185
        const val SCAN_DURATION_MS = 10_000L
    }
}
