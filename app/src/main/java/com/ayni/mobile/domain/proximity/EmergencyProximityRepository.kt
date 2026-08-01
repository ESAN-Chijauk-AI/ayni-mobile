package com.ayni.mobile.domain.proximity

import kotlinx.coroutines.flow.StateFlow

interface EmergencyProximityRepository {
    val sosStatus: StateFlow<SosModeStatus>
    val scanStatus: StateFlow<ProximityScanStatus>
    val nearbySignals: StateFlow<List<NearbySosSignal>>
    val sosReceptionState: StateFlow<SosReceptionState>
    val peerConnectionState: StateFlow<PeerConnectionState>

    fun activateSos()
    fun deactivateSos()
    fun startDetection()
    fun stopDetection()
    fun confirmAndRange(peerId: String)
    fun disconnectPeer()
}
