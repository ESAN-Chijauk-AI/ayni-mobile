package com.ayni.mobile.data.proximity

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.os.SystemClock
import com.ayni.mobile.domain.proximity.NearbySosSignal
import com.ayni.mobile.domain.proximity.ProximityScanStatus
import com.ayni.mobile.domain.proximity.RssiSignalTracker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleSosScanner @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private data class TrackedPeer(
        val tracker: RssiSignalTracker = RssiSignalTracker(),
        var latest: NearbySosSignal? = null,
    )

    private val peers = linkedMapOf<String, TrackedPeer>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val mutableSignals = MutableStateFlow<List<NearbySosSignal>>(emptyList())
    private val mutableStatus = MutableStateFlow(ProximityScanStatus.IDLE)
    private var scanCallback: ScanCallback? = null

    val signals: StateFlow<List<NearbySosSignal>> = mutableSignals.asStateFlow()
    val status: StateFlow<ProximityScanStatus> = mutableStatus.asStateFlow()

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
                mutableStatus.value = ProximityScanStatus.SCANNING
                mainHandler.post(stalePeerCheck)
            }
            .onFailure {
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
    }

    private fun ingest(result: ScanResult) {
        val payload = result.scanRecord
            ?.getServiceData(ParcelUuid(ProximityProtocol.SERVICE_UUID))
            ?: return
        if (payload.size < ProximityProtocol.PAYLOAD_SIZE ||
            payload[0] != ProximityProtocol.PROTOCOL_VERSION ||
            payload[1] != ProximityProtocol.MESSAGE_TYPE_SOS
        ) return

        val peerId = payload.copyOfRange(2, 2 + ProximityProtocol.PEER_ID_SIZE).toHex()
        val now = SystemClock.elapsedRealtime()
        synchronized(peers) {
            val tracked = peers.getOrPut(peerId) { TrackedPeer() }
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
        }
    }

    private fun publishSignals() {
        mutableSignals.value = peers.values
            .mapNotNull { it.latest }
            .sortedByDescending { it.smoothedRssi }
    }

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02X".format(it.toInt() and 0xFF) }

    private companion object {
        const val STALE_PEER_MS = 8_000L
        const val STALE_CHECK_INTERVAL_MS = 1_000L
    }
}
