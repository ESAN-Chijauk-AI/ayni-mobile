package com.ayni.mobile.data.iot.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface StructuralStateDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDevice(device: ImuDeviceEntity)

    @Query("SELECT * FROM imu_devices WHERE deviceId = :deviceId LIMIT 1")
    suspend fun findDevice(deviceId: String): ImuDeviceEntity?

    @Query(
        """
        UPDATE imu_devices
        SET displayName = CASE
                WHEN :displayName = :deviceId THEN displayName
                ELSE :displayName
            END,
            bleAddress = COALESCE(:bleAddress, bleAddress),
            protocolVersion = :protocolVersion,
            lastSeenAtEpochMs = :lastSeenAtEpochMs,
            remembered = 1,
            forgottenAtEpochMs = NULL
        WHERE deviceId = :deviceId
          AND :enrollmentStartedAtEpochMs >
              COALESCE(forgottenAtEpochMs, -1)
          AND (
              remembered = 0 OR
              bleAddress IS NULL OR
              :bleAddress IS NULL OR
              LOWER(bleAddress) = LOWER(:bleAddress)
          )
        """,
    )
    suspend fun updateDeviceRegistration(
        deviceId: String,
        displayName: String,
        bleAddress: String?,
        protocolVersion: Int,
        lastSeenAtEpochMs: Long,
        enrollmentStartedAtEpochMs: Long,
    ): Int

    @Transaction
    suspend fun explicitlyRememberDevice(
        device: ImuDeviceEntity,
        enrollmentStartedAtEpochMs: Long,
    ): Boolean {
        insertDevice(device)
        return updateDeviceRegistration(
            deviceId = device.deviceId,
            displayName = device.displayName,
            bleAddress = device.bleAddress,
            protocolVersion = device.protocolVersion,
            lastSeenAtEpochMs = device.lastSeenAtEpochMs,
            enrollmentStartedAtEpochMs = enrollmentStartedAtEpochMs,
        ) > 0
    }

    @Query(
        """
        UPDATE imu_devices
        SET lastSeenAtEpochMs = MAX(lastSeenAtEpochMs, :lastSeenAtEpochMs)
        WHERE deviceId = :deviceId
        """,
    )
    suspend fun updateDeviceTelemetry(
        deviceId: String,
        lastSeenAtEpochMs: Long,
    )

    @Query(
        """
        SELECT * FROM imu_devices
        WHERE remembered = 1
        ORDER BY lastSeenAtEpochMs DESC
        """,
    )
    fun observeRememberedDevices(): Flow<List<ImuDeviceEntity>>

    @Query(
        """
        UPDATE imu_devices
        SET remembered = 0,
            bleAddress = NULL,
            forgottenAtEpochMs = :forgottenAtEpochMs
        WHERE deviceId = :deviceId
        """,
    )
    suspend fun markDeviceForgotten(
        deviceId: String,
        forgottenAtEpochMs: Long,
    )

    @Query(
        """
        UPDATE sensor_installations
        SET active = 0
        WHERE deviceId = :deviceId AND active = 1
        """,
    )
    suspend fun closeActiveInstallations(deviceId: String)

    @Transaction
    suspend fun forgetDevice(deviceId: String, forgottenAtEpochMs: Long) {
        closeActiveInstallations(deviceId)
        markDeviceForgotten(deviceId, forgottenAtEpochMs)
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSession(session: MeasurementSessionEntity)

    @Query(
        """
        SELECT * FROM measurement_sessions
        WHERE deviceId = :deviceId AND sessionId = :sessionId
        LIMIT 1
        """,
    )
    suspend fun findSession(
        deviceId: String,
        sessionId: Long,
    ): MeasurementSessionEntity?

    @Query(
        """
        UPDATE measurement_sessions
        SET installationId = COALESCE(installationId, :installationId),
            updatedAtEpochMs = :updatedAtEpochMs,
            targetHits = :targetHits,
            complete = :complete
        WHERE deviceId = :deviceId AND sessionId = :sessionId
        """,
    )
    suspend fun updateSession(
        deviceId: String,
        sessionId: Long,
        installationId: String?,
        updatedAtEpochMs: Long,
        targetHits: Long,
        complete: Boolean,
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHit(hit: HitMeasurementEntity)

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM deleted_hits
            WHERE deviceId = :deviceId
              AND sessionId = :sessionId
              AND sequence = :sequence
        )
        """,
    )
    suspend fun isHitDeleted(
        deviceId: String,
        sessionId: Long,
        sequence: Long,
    ): Boolean

    @Query(
        """
        SELECT * FROM hit_measurements
        WHERE deviceId = :deviceId
          AND sessionId = :sessionId
          AND sequence = :sequence
        LIMIT 1
        """,
    )
    suspend fun findHit(
        deviceId: String,
        sessionId: Long,
        sequence: Long,
    ): HitMeasurementEntity?

    @Query(
        """
        SELECT * FROM structural_states
        WHERE deviceId = :deviceId
          AND sessionId = :sessionId
          AND sequence = :sequence
        LIMIT 1
        """,
    )
    suspend fun findStructuralState(
        deviceId: String,
        sessionId: Long,
        sequence: Long,
    ): StructuralStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: StructuralStateEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStructuralReading(reading: StructuralReadingEntity)

    @Query(
        """
        SELECT * FROM structural_readings
        WHERE deviceId = :deviceId
          AND installationId = :installationId
          AND receivedAtEpochMs >= :sinceEpochMs
        ORDER BY receivedAtEpochMs ASC
        LIMIT :limit
        """,
    )
    suspend fun findRecentStructuralReadings(
        deviceId: String,
        installationId: String,
        sinceEpochMs: Long,
        limit: Int,
    ): List<StructuralReadingEntity>

    @Transaction
    suspend fun persistTelemetrySnapshot(
        device: ImuDeviceEntity,
        session: MeasurementSessionEntity,
        hit: HitMeasurementEntity?,
        state: StructuralStateEntity,
    ) {
        // La telemetría puede crear la fila histórica, pero nunca reactiva un
        // sensor olvidado ni cambia su dirección BLE. El alta es otra operación.
        insertDevice(device)
        updateDeviceTelemetry(device.deviceId, device.lastSeenAtEpochMs)
        insertSession(session)
        updateSession(
            deviceId = session.deviceId,
            sessionId = session.sessionId,
            installationId = session.installationId,
            updatedAtEpochMs = session.updatedAtEpochMs,
            targetHits = session.targetHits,
            complete = session.complete,
        )
        hit?.let { incoming ->
            if (isHitDeleted(incoming.deviceId, incoming.sessionId, incoming.sequence)) {
                return@let
            }
            val previous = findHit(
                incoming.deviceId,
                incoming.sessionId,
                incoming.sequence,
            )
            upsertHit(stabilizeHitRedelivery(incoming, previous))
        }
        val previousState = findStructuralState(
            state.deviceId,
            state.sessionId,
            state.sequence,
        )
        upsert(stabilizeStructuralStateRedelivery(state, previousState))
    }

    @Query(
        """
        SELECT * FROM hit_measurements
        WHERE deviceId = :deviceId
          AND installationId = :installationId
        ORDER BY receivedAtEpochMs DESC
        """,
    )
    fun observeCandidateHits(
        deviceId: String,
        installationId: String,
    ): Flow<List<HitMeasurementEntity>>

    @Query(
        """
        SELECT * FROM hit_measurements
        WHERE deviceId = :deviceId
          AND installationId = :installationId
          AND valid = 1
        ORDER BY receivedAtEpochMs DESC
        LIMIT :limit
        """,
    )
    suspend fun findLatestValidHits(
        deviceId: String,
        installationId: String,
        limit: Int,
    ): List<HitMeasurementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeasurementTrace(trace: MeasurementTraceEntity)

    @Query(
        """
        SELECT * FROM measurement_traces
        WHERE deviceId = :deviceId
          AND sessionId = :sessionId
          AND sequence = :sequence
        LIMIT 1
        """,
    )
    suspend fun findMeasurementTrace(
        deviceId: String,
        sessionId: Long,
        sequence: Long,
    ): MeasurementTraceEntity?

    @Transaction
    suspend fun persistMeasurementTrace(trace: MeasurementTraceEntity) {
        if (!isHitDeleted(trace.deviceId, trace.sessionId, trace.sequence)) {
            val previous = findMeasurementTrace(
                trace.deviceId,
                trace.sessionId,
                trace.sequence,
            )
            upsertMeasurementTrace(stabilizeTraceRedelivery(trace, previous))
        }
    }

    @Query(
        """
        SELECT * FROM measurement_traces
        WHERE deviceId = :deviceId
          AND installationId = :installationId
        ORDER BY receivedAtEpochMs DESC
        """,
    )
    fun observeMeasurementTraces(
        deviceId: String,
        installationId: String,
    ): Flow<List<MeasurementTraceEntity>>

    @Query(
        """
        SELECT * FROM registered_states
        ORDER BY createdAtEpochMs DESC
        """,
    )
    fun observeRegisteredStates(): Flow<List<RegisteredStateEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRegisteredState(state: RegisteredStateEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRegisteredStateHits(refs: List<RegisteredStateHitCrossRef>)

    @Transaction
    suspend fun registerState(
        state: RegisteredStateEntity,
        refs: List<RegisteredStateHitCrossRef>,
    ) {
        insertRegisteredState(state)
        insertRegisteredStateHits(refs)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStructure(structure: StructureEntity)

    @Query("SELECT * FROM structures WHERE structureId = :structureId LIMIT 1")
    suspend fun findStructure(structureId: String): StructureEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInstallation(installation: SensorInstallationEntity)

    @Transaction
    suspend fun activateInstallation(
        structure: StructureEntity,
        installation: SensorInstallationEntity,
    ) {
        upsertStructure(structure)
        closeActiveInstallations(installation.deviceId)
        upsertInstallation(installation)
    }

    @Query("SELECT * FROM structures ORDER BY name")
    fun observeStructures(): Flow<List<StructureEntity>>

    @Query(
        """
        SELECT * FROM sensor_installations
        ORDER BY installedAtEpochMs DESC
        """,
    )
    fun observeInstallations(): Flow<List<SensorInstallationEntity>>

    @Query(
        """
        SELECT * FROM sensor_installations
        WHERE deviceId = :deviceId AND active = 1
        ORDER BY installedAtEpochMs DESC
        LIMIT 1
        """,
    )
    fun observeActiveInstallation(deviceId: String): Flow<SensorInstallationEntity?>

    @Query(
        """
        SELECT * FROM sensor_installations
        WHERE deviceId = :deviceId AND active = 1
        ORDER BY installedAtEpochMs DESC
        LIMIT 1
        """,
    )
    suspend fun findActiveInstallation(deviceId: String): SensorInstallationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSeismicEvent(event: SeismicEventEntity)

    @Query("SELECT * FROM seismic_events WHERE eventId = :eventId LIMIT 1")
    suspend fun findSeismicEvent(eventId: String): SeismicEventEntity?

    @Query(
        """
        SELECT * FROM seismic_events
        WHERE detectedByEsp = 1
        ORDER BY startedAtEpochMs DESC
        """,
    )
    fun observeSeismicEvents(): Flow<List<SeismicEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSeismicAnalysis(analysis: SeismicAnalysisEntity)

    @Query("SELECT * FROM seismic_analyses")
    fun observeSeismicAnalyses(): Flow<List<SeismicAnalysisEntity>>

    @Query(
        """
        SELECT 'structures' AS tableName,
               structureId AS rowKey,
               'name=' || name || ' · description=' || description AS summary,
               createdAtEpochMs AS sortAtEpochMs
        FROM structures
        UNION ALL
        SELECT 'imu_devices',
               deviceId,
               'name=' || displayName ||
                   ' · address=' || COALESCE(bleAddress, 'null') ||
                   ' · remembered=' || remembered ||
                   ' · protocol=' || protocolVersion,
               lastSeenAtEpochMs
        FROM imu_devices
        UNION ALL
        SELECT 'sensor_installations',
               installationId,
               'structure=' || substr(structureId, 1, 8) ||
                   ' · device=' || substr(deviceId, 1, 8) ||
                   ' · surface=' || surfaceType ||
                   ' · location=' || locationDescription ||
                   ' · active=' || active,
               installedAtEpochMs
        FROM sensor_installations
        UNION ALL
        SELECT 'measurement_sessions',
               deviceId || '|' || sessionId,
               'device=' || substr(deviceId, 1, 8) ||
                   ' · session=' || sessionId ||
                   ' · installation=' ||
                       COALESCE(substr(installationId, 1, 8), 'null') ||
                   ' · target=' || targetHits ||
                   ' · complete=' || complete,
               updatedAtEpochMs
        FROM measurement_sessions
        UNION ALL
        SELECT 'hit_measurements',
               deviceId || '|' || sessionId || '|' || sequence,
               'seq=' || sequence ||
                   ' · installation=' ||
                       COALESCE(substr(installationId, 1, 8), 'null') ||
                   ' · valid=' || valid ||
                   ' · fftHz=' || fftFrequencyHz ||
                   ' · mode=' || acquisitionMode ||
                   ' · reason=' || reason,
               receivedAtEpochMs
        FROM hit_measurements
        UNION ALL
        SELECT 'deleted_hits',
               deviceId || '|' || sessionId || '|' || sequence,
               'seq=' || sequence ||
                   ' · installation=' ||
                       COALESCE(substr(installationId, 1, 8), 'null') ||
                   ' · tombstone=true',
               deletedAtEpochMs
        FROM deleted_hits
        UNION ALL
        SELECT 'measurement_traces',
               deviceId || '|' || sessionId || '|' || sequence,
               'seq=' || sequence ||
                   ' · installation=' ||
                       COALESCE(substr(installationId, 1, 8), 'null') ||
                   ' · samples=' || sampleCount ||
                   ' · bytes=' || length(samplesLittleEndian) ||
                   ' · valid=' || valid ||
                   ' · diagnostic=' || rejectionCode,
               receivedAtEpochMs
        FROM measurement_traces
        UNION ALL
        SELECT 'structural_states',
               deviceId || '|' || sessionId || '|' || sequence,
               'seq=' || sequence ||
                   ' · installation=' ||
                       COALESCE(substr(installationId, 1, 8), 'null') ||
                   ' · medianHz=' || medianFrequencyHz ||
                   ' · validHits=' || validHits ||
                   ' · lastValid=' || lastHitValid,
               receivedAtEpochMs
        FROM structural_states
        UNION ALL
        SELECT 'registered_states',
               stateId,
               'name=' || name ||
                   ' · device=' || substr(deviceId, 1, 8) ||
                   ' · installation=' ||
                       COALESCE(substr(installationId, 1, 8), 'null') ||
                   ' · hits=' || hitCount ||
                   ' · medianHz=' || medianFrequencyHz,
               createdAtEpochMs
        FROM registered_states
        UNION ALL
        SELECT 'registered_state_hits',
               stateId || '|' || deviceId || '|' || sessionId || '|' || sequence,
               'state=' || substr(stateId, 1, 8) ||
                   ' · device=' || substr(deviceId, 1, 8) ||
                   ' · session=' || sessionId ||
                   ' · seq=' || sequence,
               0
        FROM registered_state_hits
        UNION ALL
        SELECT 'seismic_events',
               eventId,
               'device=' || substr(deviceId, 1, 8) ||
                   ' · installation=' ||
                       COALESCE(substr(installationId, 1, 8), 'null') ||
                   ' · sequence=' || eventSequence ||
                   ' · durationS=' || durationSeconds ||
                   ' · pgaMg=' || peakAccelerationMg,
               startedAtEpochMs
        FROM seismic_events
        UNION ALL
        SELECT 'seismic_analyses',
               eventId,
               'before=' || substr(beforeStateId, 1, 8) ||
                   ' · after=' || substr(afterStateId, 1, 8),
               updatedAtEpochMs
        FROM seismic_analyses
        ORDER BY tableName ASC, sortAtEpochMs DESC
        """,
    )
    fun observeRoomDebugRows(): Flow<List<RoomDebugRow>>

    @Query(
        """
        SELECT COUNT(*) FROM registered_state_hits
        WHERE deviceId = :deviceId
          AND sessionId = :sessionId
          AND sequence = :sequence
        """,
    )
    suspend fun countHitStateReferences(
        deviceId: String,
        sessionId: Long,
        sequence: Long,
    ): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletedHit(deleted: DeletedHitEntity)

    @Query(
        """
        DELETE FROM measurement_traces
        WHERE deviceId = :deviceId
          AND sessionId = :sessionId
          AND sequence = :sequence
        """,
    )
    suspend fun deleteTrace(
        deviceId: String,
        sessionId: Long,
        sequence: Long,
    )

    @Query(
        """
        DELETE FROM structural_states
        WHERE deviceId = :deviceId
          AND sessionId = :sessionId
          AND sequence = :sequence
        """,
    )
    suspend fun deleteStructuralSnapshot(
        deviceId: String,
        sessionId: Long,
        sequence: Long,
    )

    @Query(
        """
        DELETE FROM hit_measurements
        WHERE deviceId = :deviceId
          AND sessionId = :sessionId
          AND sequence = :sequence
        """,
    )
    suspend fun deleteHitRow(
        deviceId: String,
        sessionId: Long,
        sequence: Long,
    )

    @Transaction
    suspend fun deleteHitHistory(
        hit: HitMeasurementEntity,
        deletedAtEpochMs: Long,
    ) {
        require(
            countHitStateReferences(hit.deviceId, hit.sessionId, hit.sequence) == 0,
        ) {
            "El golpe forma parte de un estado registrado; elimina el historial " +
                "de su estructura para quitarlo"
        }
        insertDeletedHit(
            DeletedHitEntity(
                deviceId = hit.deviceId,
                sessionId = hit.sessionId,
                sequence = hit.sequence,
                installationId = hit.installationId,
                deletedAtEpochMs = deletedAtEpochMs,
            ),
        )
        deleteTrace(hit.deviceId, hit.sessionId, hit.sequence)
        deleteStructuralSnapshot(hit.deviceId, hit.sessionId, hit.sequence)
        deleteHitRow(hit.deviceId, hit.sessionId, hit.sequence)
    }

    @Query(
        """
        DELETE FROM seismic_analyses
        WHERE eventId IN (
            SELECT eventId FROM seismic_events
            WHERE installationId IN (
                SELECT installationId FROM sensor_installations
                WHERE structureId = :structureId
            )
        )
        OR beforeStateId IN (
            SELECT stateId FROM registered_states
            WHERE installationId IN (
                SELECT installationId FROM sensor_installations
                WHERE structureId = :structureId
            )
        )
        OR afterStateId IN (
            SELECT stateId FROM registered_states
            WHERE installationId IN (
                SELECT installationId FROM sensor_installations
                WHERE structureId = :structureId
            )
        )
        """,
    )
    suspend fun deleteAnalysesForStructure(structureId: String)

    @Query(
        """
        DELETE FROM registered_state_hits
        WHERE stateId IN (
            SELECT stateId FROM registered_states
            WHERE installationId IN (
                SELECT installationId FROM sensor_installations
                WHERE structureId = :structureId
            )
        )
        """,
    )
    suspend fun deleteStateRefsForStructure(structureId: String)

    @Query(
        """
        DELETE FROM measurement_traces
        WHERE installationId IN (
            SELECT installationId FROM sensor_installations
            WHERE structureId = :structureId
        )
        """,
    )
    suspend fun deleteTracesForStructure(structureId: String)

    @Query(
        """
        DELETE FROM hit_measurements
        WHERE installationId IN (
            SELECT installationId FROM sensor_installations
            WHERE structureId = :structureId
        )
        """,
    )
    suspend fun deleteHitsForStructure(structureId: String)

    @Query(
        """
        DELETE FROM deleted_hits
        WHERE installationId IN (
            SELECT installationId FROM sensor_installations
            WHERE structureId = :structureId
        )
        """,
    )
    suspend fun deleteHitTombstonesForStructure(structureId: String)

    @Query(
        """
        DELETE FROM structural_states
        WHERE installationId IN (
            SELECT installationId FROM sensor_installations
            WHERE structureId = :structureId
        )
        """,
    )
    suspend fun deleteSnapshotsForStructure(structureId: String)

    @Query(
        """
        DELETE FROM registered_states
        WHERE installationId IN (
            SELECT installationId FROM sensor_installations
            WHERE structureId = :structureId
        )
        """,
    )
    suspend fun deleteStatesForStructure(structureId: String)

    @Query(
        """
        DELETE FROM seismic_events
        WHERE installationId IN (
            SELECT installationId FROM sensor_installations
            WHERE structureId = :structureId
        )
        """,
    )
    suspend fun deleteEventsForStructure(structureId: String)

    @Query(
        """
        DELETE FROM measurement_sessions
        WHERE installationId IN (
            SELECT installationId FROM sensor_installations
            WHERE structureId = :structureId
        )
        """,
    )
    suspend fun deleteSessionsForStructure(structureId: String)

    @Query("DELETE FROM sensor_installations WHERE structureId = :structureId")
    suspend fun deleteInstallationsForStructure(structureId: String)

    @Query("DELETE FROM structures WHERE structureId = :structureId")
    suspend fun deleteStructureRow(structureId: String)

    @Transaction
    suspend fun deleteStructureHistory(structureId: String) {
        deleteAnalysesForStructure(structureId)
        deleteStateRefsForStructure(structureId)
        deleteTracesForStructure(structureId)
        deleteHitsForStructure(structureId)
        deleteHitTombstonesForStructure(structureId)
        deleteSnapshotsForStructure(structureId)
        deleteStatesForStructure(structureId)
        deleteEventsForStructure(structureId)
        deleteSessionsForStructure(structureId)
        deleteInstallationsForStructure(structureId)
        deleteStructureRow(structureId)
    }

    @Query("DELETE FROM seismic_analyses")
    suspend fun deleteAllAnalyses()

    @Query("DELETE FROM registered_state_hits")
    suspend fun deleteAllStateRefs()

    @Query("DELETE FROM measurement_traces")
    suspend fun deleteAllTraces()

    @Query("DELETE FROM hit_measurements")
    suspend fun deleteAllHits()

    @Query("DELETE FROM deleted_hits")
    suspend fun deleteAllHitTombstones()

    @Query("DELETE FROM structural_states")
    suspend fun deleteAllSnapshots()

    @Query("DELETE FROM registered_states")
    suspend fun deleteAllStates()

    @Query("DELETE FROM seismic_events")
    suspend fun deleteAllEvents()

    @Query("DELETE FROM measurement_sessions")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM sensor_installations")
    suspend fun deleteAllInstallations()

    @Query("DELETE FROM structures")
    suspend fun deleteAllStructures()

    @Transaction
    suspend fun clearAllTestHistory() {
        deleteAllAnalyses()
        deleteAllStateRefs()
        deleteAllTraces()
        deleteAllHits()
        deleteAllHitTombstones()
        deleteAllSnapshots()
        deleteAllStates()
        deleteAllEvents()
        deleteAllSessions()
        deleteAllInstallations()
        deleteAllStructures()
    }
}
