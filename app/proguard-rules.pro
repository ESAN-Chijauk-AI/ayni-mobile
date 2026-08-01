# Reglas de ProGuard/R8 para Ayni.
# Minify está desactivado para el build de hackathon (ver app/build.gradle.kts);
# este archivo queda listo para cuando se active isMinifyEnabled = true.

-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
