package com.ayni.mobile.data.iot.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Base de datos local del subsistema IoT.
 *
 * A diferencia de ProtoEstados (que arrastraba 8 versiones con migraciones a mano),
 * en ayni la BD nace desde cero: esquema en la versión 1, sin historial de migraciones.
 * Tampoco incluye la tabla médica —ese feature vive en otra parte de ayni—.
 * La instancia la construye Hilt en di/IotModule.
 */
@Database(
    entities = [
        StructureEntity::class,
        ImuDeviceEntity::class,
        SensorInstallationEntity::class,
        MeasurementSessionEntity::class,
        SeismicEventEntity::class,
        HitMeasurementEntity::class,
        DeletedHitEntity::class,
        StructuralStateEntity::class,
        RegisteredStateEntity::class,
        RegisteredStateHitCrossRef::class,
        SeismicAnalysisEntity::class,
        MeasurementTraceEntity::class,
        StructuralReadingEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class IotDatabase : RoomDatabase() {
    abstract fun structuralStateDao(): StructuralStateDao

    companion object {
        const val DATABASE_NAME = "ayni_iot.db"
    }
}
