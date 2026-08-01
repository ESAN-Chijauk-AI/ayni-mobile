package com.ayni.mobile.data.ai

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Ubicación esperada del modelo Gemma 4 E2B (`gemma-4-E2B-it.litertlm`, ~0.8GB, ya
 * cuantizado — NO recuantizar). No se empaqueta en el APK por tamaño.
 *
 * Camino principal (in-app, sin PC/adb): HomeScreen ofrece un selector de archivos del
 * sistema (Storage Access Framework) cuando el modelo no está presente — el usuario
 * elige el .litertlm desde donde lo tenga en el teléfono (Descargas, etc.) y
 * copyFromUri() lo copia al almacenamiento privado de la app. Alternativa manual por
 * PC (Android Studio Device Explorer / adb push a /data/data/com.ayni.mobile/files/)
 * sigue funcionando igual, por si prefieren esa vía.
 */
object ModelPaths {
    const val MODEL_FILENAME = "gemma-4-E2B-it.litertlm"

    fun expectedInternalPath(context: Context): File =
        File(context.filesDir, MODEL_FILENAME)

    fun isModelPresent(context: Context): Boolean =
        expectedInternalPath(context).exists()

    /**
     * Copia el archivo elegido por el usuario (URI del selector de sistema) al
     * almacenamiento privado de la app. Bloqueante — el caller debe correrlo en
     * Dispatchers.IO, nunca en Main (~0.8GB tarda varios segundos).
     */
    fun copyFromUri(context: Context, uri: Uri): Boolean = runCatching {
        val destination = expectedInternalPath(context)
        context.contentResolver.openInputStream(uri)?.use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        } ?: return false
        true
    }.getOrElse {
        expectedInternalPath(context).delete() // no dejar un archivo a medias si falló
        false
    }
}
