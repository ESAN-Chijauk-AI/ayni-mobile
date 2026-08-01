package com.ayni.mobile.domain.repository

import com.ayni.mobile.domain.model.MedicalResult
import com.ayni.mobile.domain.model.SensorReading
import com.ayni.mobile.domain.model.StructuralResult

/**
 * Puerto hacia el motor de IA local (Gemma). domain no sabe nada de LiteRT-LM,
 * MediaPipe, ni de cómo se arma el prompt — eso vive en data/ai.
 */
interface AiRepository {

    /** true una vez que el engine cargó y el warm-up terminó. */
    val isReady: Boolean

    /** Carga el engine (una sola vez) y ejecuta una inferencia dummy de calentamiento. */
    suspend fun warmUp()

    /**
     * @param imageBytes foto de la grieta sin procesar; null si no hay cámara/permiso.
     * @param sensor última lectura del acelerómetro; null si no hay sensor conectado
     * (el spec exige degradar con gracia en ese caso, no bloquear el análisis).
     */
    suspend fun analyzeStructure(imageBytes: ByteArray?, sensor: SensorReading?): StructuralResult

    suspend fun triageMedical(injuryDescription: String): MedicalResult
}
