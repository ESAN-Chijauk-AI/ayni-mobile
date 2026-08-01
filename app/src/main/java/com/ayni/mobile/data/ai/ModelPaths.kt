package com.ayni.mobile.data.ai

import android.content.Context
import java.io.File

/**
 * Ubicación esperada del modelo Gemma 4 E2B (`gemma-4-E2B-it.litertlm`, ~0.8GB, ya
 * cuantizado — NO recuantizar). No se empaqueta en el APK por tamaño; el usuario lo
 * copia manualmente al almacenamiento interno de la app antes de la primera ejecución:
 *
 *   1. Compila e instala la app una vez desde Android Studio (crea la carpeta filesDir).
 *   2. Android Studio > View > Tool Windows > Device Explorer >
 *      /data/data/com.ayni.mobile/files/  → arrastrar gemma-4-E2B-it.litertlm ahí.
 *      (Alternativa por terminal: `adb push gemma-4-E2B-it.litertlm
 *      /data/data/com.ayni.mobile/files/gemma-4-E2B-it.litertlm`, puede requerir
 *      `adb root` o `run-as com.ayni.mobile` según el dispositivo.)
 *   3. Reabrir la app: OfflineStatusBadge deja de mostrar "modelo no encontrado".
 */
object ModelPaths {
    const val MODEL_FILENAME = "gemma-4-E2B-it.litertlm"

    fun expectedInternalPath(context: Context): File =
        File(context.filesDir, MODEL_FILENAME)

    fun isModelPresent(context: Context): Boolean =
        expectedInternalPath(context).exists()
}
