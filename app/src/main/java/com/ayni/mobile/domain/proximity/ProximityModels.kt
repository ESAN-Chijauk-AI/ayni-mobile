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

enum class ProximityRole {
    SOS,
    RESCUER,
}

enum class GattConfirmationStatus {
    IDLE,
    CONNECTING,
    CONFIRMED,
    FAILED,
}

enum class RangingTechnology {
    BLE_RSSI,
    UWB,
}

enum class UwbRangingStatus {
    UNAVAILABLE,
    READY,
    NEGOTIATING,
    RANGING,
    FAILED,
}

data class SosReceptionState(
    val confirmedDetectors: Int = 0,
    val uwbAvailable: Boolean = false,
)

data class PeerConnectionState(
    val peerId: String? = null,
    val confirmationStatus: GattConfirmationStatus = GattConfirmationStatus.IDLE,
    val uwbStatus: UwbRangingStatus = UwbRangingStatus.UNAVAILABLE,
    val technology: RangingTechnology = RangingTechnology.BLE_RSSI,
    val distanceMeters: Float? = null,
    val azimuthDegrees: Float? = null,
    val elevationDegrees: Float? = null,
    val errorMessage: String? = null,
)

data class NearbySosSignal(
    val peerId: String,
    val rawRssi: Int,
    val smoothedRssi: Double,
    val level: ProximitySignalLevel,
    val trend: ProximityTrend,
    val lastSeenElapsedRealtimeMs: Long,
)
