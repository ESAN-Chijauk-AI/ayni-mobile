package com.ayni.mobile.di

import javax.inject.Qualifier

/**
 * Dispatcher para trabajo CPU/GPU-bound (inferencia de Gemma). El spec de velocidad
 * pide Dispatchers.Default para generateResponse, nunca Main. Qualifier propio en vez
 * de inyectar Dispatchers.Default directo para poder sustituirlo en tests después.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
