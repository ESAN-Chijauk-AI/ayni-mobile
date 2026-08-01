package com.ayni.mobile.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.ayni.mobile.di.DefaultDispatcher
import com.ayni.mobile.domain.model.SensorReading
import com.ayni.mobile.domain.model.StructuralHitReading
import com.ayni.mobile.domain.model.MedicalResult
import com.ayni.mobile.domain.model.StructuralResult
import com.ayni.mobile.domain.repository.AiRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

private const val TAG = "GemmaAiRepository"

/**
 * Implementación de AiRepository. Reglas aplicadas (PERQA_AGENT_RULES_GEMMA_SPEED.md):
 * - Corre en Dispatchers.Default (§8), nunca en Main.
 * - Redimensiona la imagen a máx. 768px de lado largo antes de pasarla al modelo (§9).
 * - Un reintento si el JSON no parsea, luego fallback seguro (§4/§6).
 * - Nunca lanza hacia arriba: cualquier fallo del engine (incluido el stub actual) cae
 *   al fallback, así la UI siempre recibe un resultado utilizable.
 */
@Singleton
class GemmaAiRepository @Inject constructor(
    private val engine: GemmaEngine,
    @param:ApplicationContext private val context: Context,
    @param:DefaultDispatcher private val dispatcher: CoroutineDispatcher
) : AiRepository {

    override val isReady: Boolean
        get() = engine.isReady

    override suspend fun warmUp() = withContext(dispatcher) {
        runCatching { engine.init(context) }
        runCatching { engine.generate(Prompts.SYSTEM_ESTRUCTURAL + "\nwarmup") }
        Unit
    }

    override suspend fun analyzeStructure(
        imageBytes: ByteArray?,
        sensor: SensorReading?
    ): StructuralResult = withContext(dispatcher) {
        val totalStartMs = System.currentTimeMillis()
        val resizeStartMs = System.currentTimeMillis()
        val bitmap = imageBytes?.let { resizeForModel(it, maxSide = 768) }
        if (bitmap != null) {
            Log.d(TAG, "Imagen redimensionada a ${bitmap.width}x${bitmap.height} en ${System.currentTimeMillis() - resizeStartMs}ms")
        }
        val sensorLine = sensor?.let {
            "ax=%.2f ay=%.2f az=%.2f mag=%.2f simulado=%s".format(
                it.ax, it.ay, it.az, it.magnitud, it.isSimulated
            )
        } ?: "sin dato de sensor"
        val prompt = Prompts.estructural(sensorLine)

        val parsed = generateAndParse(
            attempt = { engine.generate(prompt, bitmap) },
            parse = TriageJsonParser::parseStructural
        )

        Log.i(TAG, "analyzeStructure total: ${System.currentTimeMillis() - totalStartMs}ms (fallback=${parsed == null})")
        (parsed ?: TriageJsonParser.STRUCTURAL_FALLBACK).copy(usoSensor = sensor != null)
    }

    override suspend fun analyzeStructuralHits(
        hits: List<StructuralHitReading>,
    ): StructuralResult = withContext(dispatcher) {
        val totalStartMs = System.currentTimeMillis()
        val prompt = Prompts.golpes(hits)
        val parsed = generateAndParse(
            attempt = { engine.generate(prompt) },
            parse = TriageJsonParser::parseStructural,
        )

        Log.i(
            TAG,
            "analyzeStructuralHits total: ${System.currentTimeMillis() - totalStartMs}ms " +
                "(golpes=${hits.size}, fallback=${parsed == null})",
        )
        (parsed ?: TriageJsonParser.STRUCTURAL_FALLBACK).copy(usoSensor = true)
    }

    override suspend fun triageMedical(injuryDescription: String): MedicalResult =
        withContext(dispatcher) {
            val totalStartMs = System.currentTimeMillis()
            val prompt = Prompts.medico(injuryDescription)

            val parsed = generateAndParse(
                attempt = { engine.generate(prompt) },
                parse = TriageJsonParser::parseMedical
            )

            Log.i(TAG, "triageMedical total: ${System.currentTimeMillis() - totalStartMs}ms (fallback=${parsed == null})")
            parsed ?: TriageJsonParser.MEDICAL_FALLBACK
        }

    /** Un intento + un reintento único, según la regla de "JSON malformado -> un reintento". */
    private suspend fun <T> generateAndParse(
        attempt: suspend () -> String,
        parse: (String) -> T?
    ): T? {
        val first = runCatching { attempt() }.getOrNull()?.let(parse)
        if (first != null) return first
        Log.w(TAG, "Primer intento no parseo como JSON valido, reintentando una vez")
        val retry = runCatching { attempt() }.getOrNull()?.let(parse)
        if (retry == null) Log.w(TAG, "Reintento tambien fallo, se usara el fallback seguro")
        return retry
    }

    /** Redimensiona al lado largo <= maxSide antes de inferir (menos vision tokens, prefill rápido). */
    private fun resizeForModel(imageBytes: ByteArray, maxSide: Int): Bitmap? {
        val original = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return null
        val longSide = max(original.width, original.height)
        if (longSide <= maxSide) return original
        val scale = maxSide.toFloat() / longSide
        val targetWidth = (original.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (original.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true)
    }
}
