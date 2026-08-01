package com.ayni.mobile

import android.app.Application
import com.ayni.mobile.domain.usecase.WarmUpAiUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Dispara el warm-up de Gemma una sola vez al arrancar (F6 del spec / regla de oro
 * §7 de speed rules), en background y sin bloquear el primer frame de UI. El estado
 * "IA lista" se refleja después vía AiRepository.isReady, consumido por HomeViewModel.
 */
@HiltAndroidApp
class AyniApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Inject
    lateinit var warmUpAiUseCase: WarmUpAiUseCase

    override fun onCreate() {
        super.onCreate()
        appScope.launch { warmUpAiUseCase() }
    }
}
