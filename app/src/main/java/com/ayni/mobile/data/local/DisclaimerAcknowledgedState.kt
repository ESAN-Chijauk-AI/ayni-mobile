package com.ayni.mobile.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Flag en memoria de si el usuario ya vio/aceptó el disclaimer de seguridad (§7) en esta
 * sesión de proceso. Fuera de alcance de esta sesión persistirlo con DataStore
 * (F7/historial también quedó fuera) — el efecto práctico es que el disclaimer se
 * vuelve a mostrar en cada arranque en frío de la app, lo cual es aceptable (incluso
 * defendible) para una app de decisiones de vida o muerte: preferible pecar de
 * mostrar de más el disclaimer que de menos.
 */
@Singleton
class DisclaimerAcknowledgedState @Inject constructor() {
    private val _acknowledged = MutableStateFlow(false)
    val acknowledged: StateFlow<Boolean> = _acknowledged

    fun acknowledge() {
        _acknowledged.value = true
    }
}
