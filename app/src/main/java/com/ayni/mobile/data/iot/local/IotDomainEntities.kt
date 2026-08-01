package com.ayni.mobile.data.iot.local

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "structures")
data class StructureEntity(
    @PrimaryKey val structureId: String,
    val name: String,
    val description: String,
    val createdAtEpochMs: Long,
)

@Entity(tableName = "imu_devices")
data class ImuDeviceEntity(
    @PrimaryKey val deviceId: String,
    val displayName: String,
    val bleAddress: String?,
    val protocolVersion: Int,
    val lastSeenAtEpochMs: Long,
    @ColumnInfo(defaultValue = "1")
    val remembered: Boolean,
    @ColumnInfo(defaultValue = "0")
    val registeredAtEpochMs: Long,
    @ColumnInfo(defaultValue = "NULL")
    val forgottenAtEpochMs: Long?,
)

@Entity(
    tableName = "sensor_installations",
    indices = [
        Index("structureId"),
        Index("deviceId"),
    ],
)
data class SensorInstallationEntity(
    @PrimaryKey val installationId: String,
    val structureId: String,
    val deviceId: String,
    val surfaceType: String,
    val locationDescription: String,
    val installedAtEpochMs: Long,
    val active: Boolean,
)

@Entity(
    tableName = "measurement_sessions",
    primaryKeys = ["deviceId", "sessionId"],
)
data class MeasurementSessionEntity(
    val deviceId: String,
    val sessionId: Long,
    val installationId: String?,
    val algorithmVersion: Int,
    val startedAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val targetHits: Long,
    val complete: Boolean,
)

@Entity(
    tableName = "seismic_events",
    indices = [Index("deviceId"), Index("installationId")],
)
data class SeismicEventEntity(
    @PrimaryKey val eventId: String,
    val deviceId: String,
    val installationId: String?,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long?,
    val active: Boolean,
    val finalMedianFrequencyHz: Double,
    val finalMadHz: Double,
    val includedHits: Int,
    val totalHits: Int,
    @ColumnInfo(defaultValue = "0")
    val detectedByEsp: Boolean,
    @ColumnInfo(defaultValue = "0")
    val espSessionId: Long,
    @ColumnInfo(defaultValue = "0")
    val eventSequence: Long,
    @ColumnInfo(defaultValue = "0")
    val durationSeconds: Double,
    @ColumnInfo(defaultValue = "0")
    val peakAccelerationMg: Double,
    @ColumnInfo(defaultValue = "0")
    val rmsAccelerationMg: Double,
    @ColumnInfo(defaultValue = "0")
    val dominantFrequencyHz: Double,
    @ColumnInfo(defaultValue = "0")
    val peakAngularVelocityDps: Double,
    @ColumnInfo(defaultValue = "0")
    val rollBeforeDeg: Double,
    @ColumnInfo(defaultValue = "0")
    val pitchBeforeDeg: Double,
    @ColumnInfo(defaultValue = "0")
    val rollAfterDeg: Double,
    @ColumnInfo(defaultValue = "0")
    val pitchAfterDeg: Double,
    @ColumnInfo(defaultValue = "0")
    val tiltChangeDeg: Double,
)

@Entity(
    tableName = "hit_measurements",
    primaryKeys = ["deviceId", "sessionId", "sequence"],
    indices = [Index("eventId"), Index("installationId")],
)
data class HitMeasurementEntity(
    val deviceId: String,
    val sessionId: Long,
    val sequence: Long,
    val installationId: String?,
    val receivedAtEpochMs: Long,
    val valid: Boolean,
    val fftFrequencyHz: Double,
    val autocorrelationFrequencyHz: Double,
    val snrDb: Double,
    val periodicity: Double,
    val usefulDurationSeconds: Double,
    val peakAngularVelocityDps: Double,
    val peakDynamicAccelerationMg: Double,
    val reason: String,
    val eventId: String?,
    @ColumnInfo(defaultValue = "1")
    val includedInFinalState: Boolean,
    @ColumnInfo(defaultValue = "0")
    val rollDeg: Double,
    @ColumnInfo(defaultValue = "0")
    val pitchDeg: Double,
    @ColumnInfo(defaultValue = "0")
    val tiltChangeDeg: Double,
    // El ESP32 sólo reporta golpes controlados en `seq`/`last`: hoy esta columna
    // es siempre HITS. Se conserva como registro del origen de la captura para
    // cuando el firmware publique otros tipos de adquisición.
    @ColumnInfo(defaultValue = "'HITS'")
    val acquisitionMode: String,
) {
    fun stableKey(): String = "$deviceId|$sessionId|$sequence"
}

@Entity(
    tableName = "deleted_hits",
    primaryKeys = ["deviceId", "sessionId", "sequence"],
    indices = [Index("installationId")],
)
data class DeletedHitEntity(
    val deviceId: String,
    val sessionId: Long,
    val sequence: Long,
    val installationId: String?,
    val deletedAtEpochMs: Long,
)

@Entity(
    tableName = "registered_states",
    indices = [
        Index("deviceId"),
        Index("createdAtEpochMs"),
        Index("installationId"),
    ],
)
data class RegisteredStateEntity(
    @PrimaryKey val stateId: String,
    val deviceId: String,
    val installationId: String?,
    val name: String,
    val createdAtEpochMs: Long,
    val firstHitAtEpochMs: Long,
    val lastHitAtEpochMs: Long,
    val hitCount: Int,
    val medianFrequencyHz: Double,
    val frequencyMadHz: Double,
    val rollDeg: Double,
    val pitchDeg: Double,
    val tiltFromReferenceDeg: Double,
)

@Entity(
    tableName = "registered_state_hits",
    primaryKeys = ["stateId", "deviceId", "sessionId", "sequence"],
    indices = [
        Index("stateId"),
        Index(value = ["deviceId", "sessionId", "sequence"]),
    ],
)
data class RegisteredStateHitCrossRef(
    val stateId: String,
    val deviceId: String,
    val sessionId: Long,
    val sequence: Long,
)

@Entity(
    tableName = "seismic_analyses",
    indices = [Index("beforeStateId"), Index("afterStateId")],
)
data class SeismicAnalysisEntity(
    @PrimaryKey val eventId: String,
    val beforeStateId: String,
    val afterStateId: String,
    val updatedAtEpochMs: Long,
)
