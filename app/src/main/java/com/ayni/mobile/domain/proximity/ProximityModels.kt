package com.ayni.mobile.domain.proximity

/** Intensidad cualitativa. BLE RSSI no se convierte a metros deliberadamente. */
enum class ProximitySignalLevel {
    WEAK,
    MEDIUM,
    STRONG,
}

enum class ProximityTrend {
    APPROACHING,
    STABLE,
    MOVING_AWAY,
    UNKNOWN,
}

enum class SosModeStatus {
    INACTIVE,
    STARTING,
    ACTIVE,
    UNSUPPORTED,
    ERROR,
}

enum class ProximityScanStatus {
    IDLE,
    SCANNING,
    BLUETOOTH_UNAVAILABLE,
    PERMISSION_REQUIRED,
    ERROR,
}

data class NearbySosSignal(
    val peerId: String,
    val rawRssi: Int,
    val smoothedRssi: Double,
    val level: ProximitySignalLevel,
    val trend: ProximityTrend,
    val lastSeenElapsedRealtimeMs: Long,
)

