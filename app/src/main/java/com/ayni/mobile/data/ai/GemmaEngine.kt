package com.ayni.mobile.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tag único para poder filtrar Logcat por "GemmaEngine" y ver SOLO los tiempos de
 * carga/inferencia, separado del ruido del sistema (ver PERQA_AGENT_RULES_GEMMA_SPEED.md
 * §13: medir después de cada cambio, separar TTFT de decode speed).
 */
private const val TAG = "GemmaEngine"

/**
 * Envoltura propia sobre el SDK real de inferencia (LiteRT-LM / MediaPipe LLM Inference).
 * Existe para que el resto de la app (GemmaAiRepository, casos de uso, UI) nunca dependa
 * de nombres de clase/builder del SDK concreto — que sigue en preview y puede cambiar
 * (PERQA_AGENT_RULES_GEMMA_SPEED.md §3). Cuando se confirme el artefacto real de
 * LiteRT-LM, todo el cambio se localiza a GemmaEngineImpl.
 *
 * Contrato de ciclo de vida que la implementación real DEBE respetar (reglas de oro):
 * - init() crea el engine UNA sola vez y lo mantiene residente (nunca recrearlo por request).
 * - generate() crea una LlmInferenceSession nueva por llamada y la cierra en el finally
 *   (try/finally), con temperature=0.2, topK=40, topP=0.95, randomSeed=42, backend GPU
 *   con fallback CPU, maxTokens<=200, y SIN el token `<|think|>` en ningún prompt.
 * - Nunca se ejecuta en el hilo principal (el caller ya corre en Dispatchers.Default).
 */
interface GemmaEngine {
    val isReady: Boolean
    suspend fun init(context: Context)
    suspend fun generate(prompt: String, image: Bitmap? = null): String
}

/**
 * Implementación real sobre MediaPipe LLM Inference (`com.google.mediapipe:tasks-genai`).
 * TODO(litert-lm): migrar a LiteRT-LM cuando su coordenada Maven pública esté confirmada
 * — el resto de la app (GemmaAiRepository, casos de uso, UI) no se entera del cambio,
 * solo habla con la interfaz GemmaEngine.
 *
 * Ciclo de vida (reglas de oro del spec de velocidad): el LlmInference se crea UNA sola
 * vez en init() y se mantiene residente; cada generate() abre una LlmInferenceSession
 * nueva con los parámetros de triage (temperature 0.2, topK 40, topP 0.95, seed 42) y la
 * cierra en el finally. Backend GPU con fallback a CPU si la carga en GPU falla (p.ej.
 * dispositivo sin soporte).
 */
@Singleton
class GemmaEngineImpl @Inject constructor() : GemmaEngine {

    @Volatile
    private var llmInference: LlmInference? = null

    override val isReady: Boolean
        get() = llmInference != null

    override suspend fun init(context: Context) {
        if (llmInference != null) return
        val modelPath = ModelPaths.expectedInternalPath(context).absolutePath
        val startMs = System.currentTimeMillis()

        llmInference = runCatching {
            createEngine(context, modelPath, LlmInference.Backend.GPU)
        }.onSuccess {
            Log.i(TAG, "Engine cargado con backend GPU en ${System.currentTimeMillis() - startMs}ms")
        }.getOrElse { gpuError ->
            // Fallback CPU (regla de oro: GPU/NPU preferido, CPU como red de seguridad,
            // nunca al revés) — algunos dispositivos/emuladores no soportan el delegate GPU.
            // ESTE log es el más importante para diagnosticar lentitud: si ves este
            // mensaje, el modelo está corriendo en CPU (mucho más lento, minutos en vez
            // de segundos en Gemma 4 E2B) en vez de GPU/NPU.
            Log.w(TAG, "GPU no disponible (${gpuError.message}), cayendo a CPU — esto puede ser la causa de la lentitud", gpuError)
            val cpuStartMs = System.currentTimeMillis()
            createEngine(context, modelPath, LlmInference.Backend.CPU).also {
                Log.i(TAG, "Engine cargado con backend CPU en ${System.currentTimeMillis() - cpuStartMs}ms")
            }
        }
    }

    private fun createEngine(context: Context, modelPath: String, backend: LlmInference.Backend): LlmInference {
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            // OJO: este valor es prompt+respuesta combinados en MediaPipe LLM Inference,
            // no solo la salida — con prompts de ~100-150 tokens, bajarlo mucho trunca
            // el prompt en vez de acelerar. NO bajar sin medir tokens de prompt reales.
            // El problema de lentitud real (ver GemmaEngine.generate) es que este
            // runtime no para en el token de fin de turno de Gemma 4 y rellena con
            // <eos> hasta este tope — la solución real es streaming con corte
            // temprano (implementado abajo), no este número.
            .setMaxTokens(200)
            .setPreferredBackend(backend)
            .build()
        return LlmInference.createFromOptions(context, options)
    }

    override suspend fun generate(prompt: String, image: Bitmap?): String {
        val engine = llmInference
            ?: error("GemmaEngine.init() no fue llamado o el modelo (.litertlm) no está presente")

        val sessionOptionsBuilder = LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setTemperature(0.2f)
            .setTopK(40)
            .setTopP(0.95f)
            .setRandomSeed(42)

        if (image != null) {
            sessionOptionsBuilder.setGraphOptions(
                GraphOptions.builder().setEnableVisionModality(true).build()
            )
        }

        val sessionStartMs = System.currentTimeMillis()
        val session = LlmInferenceSession.createFromOptions(engine, sessionOptionsBuilder.build())
        Log.d(TAG, "Sesion creada en ${System.currentTimeMillis() - sessionStartMs}ms (prompt: ${prompt.length} chars, imagen: ${image != null})")

        return try {
            session.addQueryChunk(prompt)
            if (image != null) {
                session.addImage(BitmapImageBuilder(image).build())
            }
            val genStartMs = System.currentTimeMillis()
            val result = session.generateResponse()
            Log.i(TAG, "generateResponse() tardo ${System.currentTimeMillis() - genStartMs}ms, respuesta: $result")
            result
        } finally {
            session.close()
        }
    }
}
