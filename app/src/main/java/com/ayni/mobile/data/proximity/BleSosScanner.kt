package com.ayni.mobile.data.proximity

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.os.SystemClock
import android.util.Log
import com.ayni.mobile.domain.proximity.NearbySosSignal
import com.ayni.mobile.domain.proximity.ProximityScanStatus
import com.ayni.mobile.domain.proximity.RssiSignalTracker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AyniSosScanner"

@Singleton
class BleSosScanner @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private data class TrackedPeer(
        val tracker: RssiSignalTracker = RssiSignalTracker(),
        var device: BluetoothDevice? = null,
        var latest: NearbySosSignal? = null,
    )

    private val peers = linkedMapOf<String, TrackedPeer>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val mutableSignals = MutableStateFlow<List<NearbySosSignal>>(emptyList())
    private val mutableStatus = MutableStateFlow(ProximityScanStatus.IDLE)
    private var scanCallback: ScanCallback? = null

    val signals: StateFlow<List<NearbySosSignal>> = mutableSignals.asStateFlow()
    val status: StateFlow<ProximityScanStatus> = mutableStatus.asStateFlow()

    fun deviceFor(peerId: String): BluetoothDevice? = synchronized(peers) {
        peers[peerId]?.device
    }

    private val stalePeerCheck = object : Runnable {
        override fun run() {
            val now = SystemClock.elapsedRealtime()
            synchronized(peers) {
                peers.entries.removeAll { (_, peer) ->
                    val lastSeen = peer.latest?.lastSeenElapsedRealtimeMs ?: 0L
                    now - lastSeen > STALE_PEER_MS
                }
                publishSignals()
            }
            if (scanCallback != null) mainHandler.postDelayed(this, STALE_CHECK_INTERVAL_MS)
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (scanCallback != null) return
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
        val scanner = runCatching { adapter?.bluetoothLeScanner }.getOrNull()
        if (adapter == null || !adapter.isEnabled || scanner == null) {
            Log.e(TAG, "BLE scanner no disponible o Bluetooth apagado")
            mutableStatus.value = ProximityScanStatus.BLUETOOTH_UNAVAILABLE
            return
        }

        synchronized(peers) {
            peers.clear()
            publishSignals()
        }
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                ingest(result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach(::ingest)
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "BLE scan falló; errorCode=$errorCode")
                scanCallback = null
                mainHandler.removeCallbacks(stalePeerCheck)
                mutableStatus.value = ProximityScanStatus.ERROR
            }
        }
        scanCallback = callback

        val uuid = ParcelUuid(ProximityProtocol.SERVICE_UUID)
        val filter = ScanFilter.Builder().setServiceUuid(uuid).build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()
        runCatching { scanner.startScan(listOf(filter), settings, callback) }
            .onSuccess {
                Log.i(TAG, "Detector BLE iniciado; buscando UUID=${ProximityProtocol.SERVICE_UUID}")
                mutableStatus.value = ProximityScanStatus.SCANNING
                mainHandler.post(stalePeerCheck)
            }
            .onFailure {
                Log.e(TAG, "Excepción al iniciar BLE scan", it)
                scanCallback = null
                mutableStatus.value = if (it is SecurityException) {
                    ProximityScanStatus.PERMISSION_REQUIRED
                } else {
                    ProximityScanStatus.ERROR
                }
            }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        val callback = scanCallback
        scanCallback = null
        mainHandler.removeCallbacks(stalePeerCheck)
        if (callback != null) {
            val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
            runCatching { adapter?.bluetoothLeScanner?.stopScan(callback) }
        }
        synchronized(peers) {
            peers.clear()
            publishSignals()
        }
        mutableStatus.value = ProximityScanStatus.IDLE
        Log.i(TAG, "Detector BLE detenido")
    }

    @SuppressLint("MissingPermission")
    private fun ingest(result: ScanResult) {
        val uuid = ParcelUuid(ProximityProtocol.SERVICE_UUID)
        if (uuid !in result.scanRecord?.serviceUuids.orEmpty()) return

        // La dirección BLE puede ser aleatoria. Se transforma en una huella corta sólo
        // para fijar el objetivo durante esta búsqueda; no se persiste ni se muestra.
        val peerId = result.device.address.sessionFingerprint()
        val now = SystemClock.elapsedRealtime()
        synchronized(peers) {
            val isNewPeer = peerId !in peers
            val tracked = peers.getOrPut(peerId) { TrackedPeer() }
            tracked.device = result.device
            val estimate = tracked.tracker.add(result.rssi, now)
            tracked.latest = NearbySosSignal(
                peerId = peerId,
                rawRssi = result.rssi,
                smoothedRssi = estimate.smoothedRssi,
                level = estimate.level,
                trend = estimate.trend,
                lastSeenElapsedRealtimeMs = now,
            )
            publishSignals()
            if (isNewPeer) {
                Log.i(TAG, "Baliza SOS Ayni encontrada; peer=$peerId rssi=${result.rssi}")
            }
        }
    }

    private fun publishSignals() {
        mutableSignals.value = peers.values
            .mapNotNull { it.latest }
            .sortedByDescending { it.smoothedRssi }
    }

    private fun String.sessionFingerprint(): String = MessageDigest
        .getInstance("SHA-256")
        .digest(toByteArray(Charsets.UTF_8))
        .take(4)
        .joinToString("") { "%02X".format(it.toInt() and 0xFF) }

    private companion object {
        const val STALE_PEER_MS = 8_000L
        const val STALE_CHECK_INTERVAL_MS = 1_000L
    }
}
