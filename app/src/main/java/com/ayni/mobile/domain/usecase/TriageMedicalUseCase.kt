package com.ayni.mobile.domain.usecase

import com.ayni.mobile.domain.model.MedicalResult
import com.ayni.mobile.domain.repository.AiRepository
import javax.inject.Inject

class TriageMedicalUseCase @Inject constructor(
    private val aiRepository: AiRepository
) {
    suspend operator fun invoke(injuryDescription: String, imageBytes: ByteArray? = null): MedicalResult =
        aiRepository.triageMedical(injuryDescription, imageBytes)
}
