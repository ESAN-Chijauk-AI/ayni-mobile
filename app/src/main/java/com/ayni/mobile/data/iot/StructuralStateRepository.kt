package com.ayni.mobile.data.iot

import com.ayni.mobile.data.iot.local.HitMeasurementEntity
import com.ayni.mobile.data.iot.local.ImuDeviceEntity
import com.ayni.mobile.data.iot.local.MeasurementSessionEntity
import com.ayni.mobile.data.iot.local.RegisteredStateEntity
import com.ayni.mobile.data.iot.local.RegisteredStateHitCrossRef
import com.ayni.mobile.data.iot.local.SeismicAnalysisEntity
import com.ayni.mobile.data.iot.local.SeismicEventEntity
import com.ayni.mobile.data.iot.local.SensorInstallationEntity
import com.ayni.mobile.data.iot.local.StructuralReadingEntity
import com.ayni.mobile.data.iot.local.StructuralStateDao
import com.ayni.mobile.data.iot.local.StructureEntity
import com.ayni.mobile.data.iot.local.toEntity
import com.ayni.mobile.domain.iot.MeasurementMode
import com.ayni.mobile.domain.iot.MeasurementTrace
import com.ayni.mobile.domain.iot.RobustStatistics
import com.ayni.mobile.domain.iot.SeismicEventRecord
import com.ayni.mobile.domain.iot.StructuralSnapshot
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

class DeviceIdentityConflictException(
    val deviceId: String,
    val knownAddress: String,
    val observedAddress: String,
) : IllegalStateException(
    "El UUID $deviceId ya está registrado en $knownAddress y apareció en $observedAddress",
)

class DeviceEnrollmentSupersededException(deviceId: String) :
    IllegalStateException(
        "El alta de $deviceId fue cancelada por un olvido o una conexión más reciente",
    )

@Singleton
class StructuralStateRepository @Inject constructor(
    private val dao: StructuralStateDao,
) {
    val rememberedDevices = dao.observeRememberedDevices()
    val structures = dao.observeStructures()
    val installations = dao.observeInstallations()
    val registeredStates = dao.observeRegisteredStates()
    val seismicEvents = dao.observeSeismicEvents()
    val seismicAnalyses = dao.observeSeismicAnalyses()
    val roomDebugRows = dao.observeRoomDebugRows()

    fun observeActiveInstallation(deviceId: String) =
        dao.observeActiveInstallation(deviceId)

    fun observeCandidateHits(
        deviceId: String,
        installationId: String,
    ) = dao.observeCandidateHits(deviceId, installationId)

    suspend fun findLatestValidHits(
        deviceId: String,
        installationId: String,
        limit: Int = 10,
    ): List<HitMeasurementEntity> = dao.findLatestValidHits(
        deviceId = deviceId,
        installationId = installationId,
        limit = limit,
    )

    fun observeMeasurementTraces(
        deviceId: String,
        installationId: String,
    ) = dao.observeMeasurementTraces(deviceId, installationId)

    suspend fun findActiveInstallation(deviceId: String) =
        dao.findActiveInstallation(deviceId)

    suspend fun findRecentStructuralReadings(
        deviceId: String,
        installationId: String,
        sinceEpochMs: Long,
        limit: Int = 300,
    ): List<StructuralReadingEntity> =
        dao.findRecentStructuralReadings(
            deviceId = deviceId,
            installationId = installationId,
            sinceEpochMs = sinceEpochMs,
            limit = limit,
        )

    suspend fun persistMeasurementTrace(
        trace: MeasurementTrace,
        activeInstallationId: String?,
    ) {
        val installationId = resolveInstallationId(
            deviceId = trace.deviceId,
            sessionId = trace.sessionId,
            activeInstallationId = activeInstallationId,
        )
        dao.persistMeasurementTrace(
            trace.toEntity(installationId = installationId),
        )
    }

    suspend fun persistSnapshot(
        snapshot: StructuralSnapshot,
        activeInstallationId: String?,
    ) {
        val now = System.currentTimeMillis()
        val installationId = resolveInstallationId(
            deviceId = snapshot.deviceId,
            sessionId = snapshot.sessionId,
            activeInstallationId = activeInstallationId,
        )
        dao.persistTelemetrySnapshot(
            device = ImuDeviceEntity(
                deviceId = snapshot.deviceId,
                displayName = snapshot.deviceId,
                bleAddress = null,
                protocolVersion = 1,
                lastSeenAtEpochMs = now,
                remembered = false,
                registeredAtEpochMs = 0,
                forgottenAtEpochMs = null,
            ),
            session = MeasurementSessionEntity(
                deviceId = snapshot.deviceId,
                sessionId = snapshot.sessionId,
                installationId = installationId,
                algorithmVersion = 1,
                startedAtEpochMs = now,
                updatedAtEpochMs = now,
                targetHits = 0,
                complete = false,
            ),
            // En el protocolo actual `seq` y `last` pertenecen siempre al último
            // golpe controlado. El modo operativo al recibir una retransmisión
            // puede ser REST o SEISMIC y no describe el origen de la captura.
            hit = snapshot.takeIf { it.sequence > 0 }
                ?.let {
                    HitMeasurementEntity(
                        deviceId = it.deviceId,
                        sessionId = it.sessionId,
                        sequence = it.sequence,
                        installationId = installationId,
                        receivedAtEpochMs = now,
                        valid = it.lastHitValid,
                        fftFrequencyHz = it.fftFrequencyHz,
                        autocorrelationFrequencyHz = it.autocorrelationFrequencyHz,
                        snrDb = it.snrDb,
                        periodicity = it.periodicity,
                        usefulDurationSeconds = it.usefulDurationSeconds,
                        peakAngularVelocityDps = it.peakAngularVelocityDps,
                        peakDynamicAccelerationMg = it.peakDynamicAccelerationMg,
                        reason = it.resultReason,
                        eventId = null,
                        includedInFinalState = false,
                        rollDeg = it.currentRollDeg,
                        pitchDeg = it.currentPitchDeg,
                        tiltChangeDeg = it.tiltChangeDeg,
                        acquisitionMode = MeasurementMode.HITS.name,
                    )
                },
            state = snapshot.toEntity(now, installationId),
        )
    }

    suspend fun persistStructuralReading(
        snapshot: StructuralSnapshot,
        installationId: String,
        receivedAtEpochMs: Long = System.currentTimeMillis(),
    ) {
        dao.insertStructuralReading(
            StructuralReadingEntity(
                deviceId = snapshot.deviceId,
                installationId = installationId,
                receivedAtEpochMs = receivedAtEpochMs,
                medianFrequencyHz = snapshot.medianFrequencyHz,
                frequencyMadHz = snapshot.frequencyMadHz,
                snrDb = snapshot.snrDb,
                periodicity = snapshot.periodicity,
                tiltChangeDeg = snapshot.tiltChangeDeg,
                orientationConfidence = snapshot.orientationConfidence,
                peakAngularVelocityDps = snapshot.peakAngularVelocityDps,
                peakDynamicAccelerationMg = snapshot.peakDynamicAccelerationMg,
                abruptMovement = snapshot.abruptMovement,
                accelerometerNoiseMg = snapshot.accelerometerNoiseMg,
                temperatureC = snapshot.temperatureC,
                lastHitValid = snapshot.lastHitValid,
                resultReason = snapshot.resultReason,
            ),
        )
    }

    suspend fun explicitlyRegisterDevice(
        deviceId: String,
        displayName: String,
        bleAddress: String?,
        enrollmentStartedAtEpochMs: Long,
    ) {
        val now = System.currentTimeMillis()
        val existing = dao.findDevice(deviceId)
        val knownAddress = existing
            ?.takeIf { it.remembered }
            ?.bleAddress
        if (
            knownAddress != null &&
            bleAddress != null &&
            !knownAddress.equals(bleAddress, ignoreCase = true)
        ) {
            throw DeviceIdentityConflictException(
                deviceId = deviceId,
                knownAddress = knownAddress,
                observedAddress = bleAddress,
            )
        }

        val registered = dao.explicitlyRememberDevice(
            device = ImuDeviceEntity(
                deviceId = deviceId,
                displayName = displayName,
                bleAddress = bleAddress,
                protocolVersion = 1,
                lastSeenAtEpochMs = now,
                remembered = true,
                registeredAtEpochMs = enrollmentStartedAtEpochMs,
                forgottenAtEpochMs = null,
            ),
            enrollmentStartedAtEpochMs = enrollmentStartedAtEpochMs,
        )
        if (!registered) {
            // Relee para distinguir una dirección conflictiva de un alta que
            // perdió la carrera contra "olvidar". El WHERE del DAO protege la
            // base aun si este callback empezó antes del olvido.
            val currentAddress = dao.findDevice(deviceId)
                ?.takeIf { it.remembered }
                ?.bleAddress
            if (
                currentAddress != null &&
                bleAddress != null &&
                !currentAddress.equals(bleAddress, ignoreCase = true)
            ) {
                throw DeviceIdentityConflictException(
                    deviceId = deviceId,
                    knownAddress = currentAddress,
                    observedAddress = bleAddress,
                )
            }
            throw DeviceEnrollmentSupersededException(deviceId)
        }
    }

    suspend fun forgetDevice(deviceId: String) {
        dao.forgetDevice(
            deviceId = deviceId,
            forgottenAtEpochMs = System.currentTimeMillis(),
        )
    }

    suspend fun startInstallation(
        deviceId: String,
        existingStructureId: String?,
        structureName: String,
        structureDescription: String,
        surfaceType: String,
        locationDescription: String,
    ): SensorInstallationEntity {
        val now = System.currentTimeMillis()
        val structure = if (existingStructureId != null) {
            requireNotNull(dao.findStructure(existingStructureId)) {
                "La estructura seleccionada ya no existe"
            }
        } else {
            require(structureName.isNotBlank()) {
                "Escribe un nombre para la nueva estructura"
            }
            StructureEntity(
                structureId = UUID.randomUUID().toString(),
                name = structureName.trim(),
                description = structureDescription.trim(),
                createdAtEpochMs = now,
            )
        }
        val installation = SensorInstallationEntity(
            installationId = UUID.randomUUID().toString(),
            structureId = structure.structureId,
            deviceId = deviceId,
            surfaceType = surfaceType.trim().ifBlank { "No especificada" },
            locationDescription = locationDescription.trim(),
            installedAtEpochMs = now,
            active = true,
        )
        dao.activateInstallation(structure, installation)
        return installation
    }

    suspend fun deleteHit(hit: HitMeasurementEntity) {
        dao.deleteHitHistory(hit, System.currentTimeMillis())
    }

    suspend fun deleteStructureHistory(structureId: String) {
        requireNotNull(dao.findStructure(structureId)) {
            "La estructura ya no existe"
        }
        dao.deleteStructureHistory(structureId)
    }

    suspend fun clearAllTestHistory() {
        dao.clearAllTestHistory()
    }

    suspend fun registerState(
        name: String,
        selectedHits: List<HitMeasurementEntity>,
        expectedDeviceId: String,
        expectedInstallationId: String,
    ): RegisteredStateEntity {
        val hits = selectedHits
            .filter { it.valid }
            .distinctBy { Triple(it.deviceId, it.sessionId, it.sequence) }
        require(hits.isNotEmpty()) { "Selecciona al menos un golpe válido" }
        val deviceId = hits.first().deviceId
        require(deviceId == expectedDeviceId && hits.all { it.deviceId == deviceId }) {
            "Todos los golpes deben pertenecer al mismo sensor"
        }
        require(
            hits.all { it.installationId == expectedInstallationId },
        ) {
            "Todos los golpes deben pertenecer al mismo montaje"
        }

        val frequency = RobustStatistics.summarize(hits.map { it.fftFrequencyHz })
        val state = RegisteredStateEntity(
            stateId = UUID.randomUUID().toString(),
            deviceId = deviceId,
            installationId = expectedInstallationId,
            name = name.trim().ifBlank { "Estado ${System.currentTimeMillis()}" },
            createdAtEpochMs = hits.maxOf { it.receivedAtEpochMs },
            firstHitAtEpochMs = hits.minOf { it.receivedAtEpochMs },
            lastHitAtEpochMs = hits.maxOf { it.receivedAtEpochMs },
            hitCount = hits.size,
            medianFrequencyHz = frequency.medianHz,
            frequencyMadHz = frequency.madHz,
            rollDeg = RobustStatistics.circularMeanDegrees(hits.map { it.rollDeg }),
            pitchDeg = RobustStatistics.medianValue(hits.map { it.pitchDeg }),
            tiltFromReferenceDeg =
                RobustStatistics.medianValue(hits.map { it.tiltChangeDeg }),
        )
        val refs = hits.map {
            RegisteredStateHitCrossRef(
                stateId = state.stateId,
                deviceId = it.deviceId,
                sessionId = it.sessionId,
                sequence = it.sequence,
            )
        }
        dao.registerState(state, refs)
        return state
    }

    suspend fun persistSeismicEvent(
        event: SeismicEventRecord,
        activeInstallationId: String?,
    ) {
        val eventId =
            "${event.deviceId}|${event.espSessionId}|${event.eventSequence}"
        val endedAt = System.currentTimeMillis()
        val startedAt = endedAt - (event.durationSeconds * 1000.0).toLong()
        val previous = dao.findSeismicEvent(eventId)
        val installationId = previous?.installationId ?: resolveInstallationId(
            deviceId = event.deviceId,
            sessionId = event.espSessionId,
            activeInstallationId = activeInstallationId,
        )
        dao.upsertSeismicEvent(
            SeismicEventEntity(
                eventId = eventId,
                deviceId = event.deviceId,
                installationId = installationId,
                startedAtEpochMs = previous?.startedAtEpochMs ?: startedAt,
                endedAtEpochMs = previous?.endedAtEpochMs ?: endedAt,
                active = false,
                finalMedianFrequencyHz = 0.0,
                finalMadHz = 0.0,
                includedHits = 0,
                totalHits = 0,
                detectedByEsp = true,
                espSessionId = event.espSessionId,
                eventSequence = event.eventSequence,
                durationSeconds = event.durationSeconds,
                peakAccelerationMg = event.peakAccelerationMg,
                rmsAccelerationMg = event.rmsAccelerationMg,
                dominantFrequencyHz = event.dominantFrequencyHz,
                peakAngularVelocityDps = event.peakAngularVelocityDps,
                rollBeforeDeg = event.rollBeforeDeg,
                pitchBeforeDeg = event.pitchBeforeDeg,
                rollAfterDeg = event.rollAfterDeg,
                pitchAfterDeg = event.pitchAfterDeg,
                tiltChangeDeg = event.tiltChangeDeg,
            ),
        )
    }

    suspend fun saveSeismicAnalysis(
        eventId: String,
        beforeStateId: String,
        afterStateId: String,
    ) {
        require(beforeStateId != afterStateId) {
            "Selecciona estados diferentes"
        }
        dao.upsertSeismicAnalysis(
            SeismicAnalysisEntity(
                eventId = eventId,
                beforeStateId = beforeStateId,
                afterStateId = afterStateId,
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    private suspend fun resolveInstallationId(
        deviceId: String,
        sessionId: Long,
        activeInstallationId: String?,
    ): String? {
        val existingSession = dao.findSession(deviceId, sessionId)
        return if (existingSession == null) {
            activeInstallationId
        } else {
            existingSession.installationId
        }
    }
}
