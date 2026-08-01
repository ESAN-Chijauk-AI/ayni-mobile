package com.ayni.mobile.domain.usecase

import com.ayni.mobile.domain.proximity.EmergencyProximityRepository
import javax.inject.Inject

class ManageEmergencyProximityUseCase @Inject constructor(
    private val repository: EmergencyProximityRepository,
) {
    val sosStatus = repository.sosStatus
    val scanStatus = repository.scanStatus
    val nearbySignals = repository.nearbySignals

    fun activateSos() = repository.activateSos()
    fun deactivateSos() = repository.deactivateSos()
    fun startDetection() = repository.startDetection()
    fun stopDetection() = repository.stopDetection()
}
