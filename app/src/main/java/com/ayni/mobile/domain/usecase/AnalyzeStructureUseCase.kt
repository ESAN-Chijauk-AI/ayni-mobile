package com.ayni.mobile.domain.usecase

import com.ayni.mobile.domain.model.SensorReading
import com.ayni.mobile.domain.model.StructuralResult
import com.ayni.mobile.domain.repository.AiRepository
import javax.inject.Inject

class AnalyzeStructureUseCase @Inject constructor(
    private val aiRepository: AiRepository
) {
    suspend operator fun invoke(imageBytes: ByteArray?, sensor: SensorReading?): StructuralResult =
        aiRepository.analyzeStructure(imageBytes, sensor)
}
