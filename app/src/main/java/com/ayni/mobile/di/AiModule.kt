package com.ayni.mobile.di

import com.ayni.mobile.data.ai.GemmaAiRepository
import com.ayni.mobile.data.ai.GemmaEngine
import com.ayni.mobile.data.ai.GemmaEngineImpl
import com.ayni.mobile.domain.repository.AiRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Engine único residente (regla de oro del spec de velocidad: "MUST cargar el
 * LlmInferenceEngine una sola vez y mantenerlo residente"). @Singleton en ambos
 * bindings asegura una sola instancia por proceso.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindGemmaEngine(impl: GemmaEngineImpl): GemmaEngine

    @Binds
    @Singleton
    abstract fun bindAiRepository(impl: GemmaAiRepository): AiRepository
}
