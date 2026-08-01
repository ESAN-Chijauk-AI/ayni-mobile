package com.ayni.mobile.domain.usecase

import com.ayni.mobile.domain.repository.AiRepository
import javax.inject.Inject

/** Se dispara una sola vez desde AyniApplication.onCreate(), fuera del hilo de UI. */
class WarmUpAiUseCase @Inject constructor(
    private val aiRepository: AiRepository
) {
    suspend operator fun invoke() = aiRepository.warmUp()
}
