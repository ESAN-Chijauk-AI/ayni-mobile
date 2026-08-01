# ayni-mobile — App Android nativa (Kotlin)

App móvil offline-first de respuesta post-sismo con dos módulos de triage con IA local
(Gemma 4 E2B): **estructural** (foto de grieta + acelerómetro ESP32/MPU6050 → semáforo
VERDE/AMARILLO/ROJO estilo ATC-20) y **médico** (lesión → prioridad START). Nombre de
producto: **Ayni**. `applicationId`/namespace: `com.ayni.mobile`.

## Fuente de verdad — leer antes de tocar código

- `../documentation/PERQA_AGENT_BUILD_SPEC.md` — arquitectura, features, stack
  aprobado, sistema de diseño, seguridad de contenido. **No negociable salvo
  justificación explícita en el PR.**
- `../documentation/PERQA_AGENT_RULES_GEMMA_SPEED.md` — reglas de rendimiento de la
  integración de Gemma (thinking off, engine único, JSON capado, warm-up, etc.).
- `../documentation/RUBRICA.MD` — checklist de validación de la submission del hackathon.

## Arquitectura (obligatoria, no reinterpretar)

Capas unidireccionales UI → domain → data, MVVM + UDF con `StateFlow`.

```
ui/domain/data/di   bajo app/src/main/java/com/ayni/mobile/
```

Reglas duras que cualquier PR debe respetar:
- `domain/**` es Kotlin puro: **cero** imports de `android.*`, `androidx.*` o Compose.
- La UI nunca llama a `GemmaEngine` ni a `SensorRepository` directo — solo a casos de
  uso del `domain` (`AnalyzeStructureUseCase`, `TriageMedicalUseCase`, `WarmUpAiUseCase`).
- Estado de pantalla = `data class`/`sealed interface` inmutable expuesto como
  `StateFlow` desde el ViewModel. Nunca `LiveData`, nunca estado mutable expuesto.
- Cero lógica de negocio en Composables. Cero llamadas de IA/sensor en el hilo de UI
  (`Dispatchers.Default`, nunca Main).
- Cero dependencia de red (`Retrofit`/`OkHttp`/`java.net`) en `data/ai` ni `domain`.
  Ayni es offline-first real; el `AndroidManifest.xml` **no** declara `INTERNET`.

## Stack aprobado

Ver `PERQA_AGENT_BUILD_SPEC.md` §2 para la lista completa y la justificación (solo
librerías oficiales/reconocidas, Apache-2.0/MIT, activas). No agregar dependencias
fuera de esa lista sin discutirlo antes — especialmente nada de SDKs de analytics/ads
ni clientes HTTP en el camino de triage.

## Estado actual (actualizar esta sección al final de cada sesión de trabajo grande)

Construido en la sesión inicial (andamiaje completo desde repo vacío):
- Proyecto Gradle Kotlin DSL + Version Catalog, Compose/Material3, Hilt, Navigation.
- `domain` completo (modelos, interfaces de repositorio, casos de uso).
- `data/ai`: prompts, parser JSON con retry+fallback, `GemmaEngine` (interfaz) con
  `GemmaEngineImpl` **stub** — ver `TODO(litert-lm)` en `data/ai/GemmaEngine.kt`.
- `data/sensor`: `MockSensorRepository` funcional (12Hz, simula réplica), `BleSensorRepository`
  **stub sin lógica real** (F4 despriorizado a propósito, ver `TODO(ble)` ahí).
- Theme dark-first con los tokens exactos del spec §6.2. Fuentes son placeholders del
  sistema (`FontFamily.SansSerif`/`Monospace`) — ver `TODO(fonts)` en `ui/theme/Type.kt`.
- Flujos completos: Home (F1), Triage Estructural con CameraX (F2), Triage Médico (F3),
  Resultado con semáforo+haptics (F5), estado de sensor (F4 mock), disclaimer/onboarding (§7).

Pendiente (en orden de impacto):
1. **Integrar el SDK real de Gemma** en `GemmaEngineImpl` (LiteRT-LM cuando su artefacto
   Maven público esté confirmado, o MediaPipe `tasks-genai` ya declarado en Gradle).
2. Colocar el modelo `gemma-4-E2B-it.litertlm` en el dispositivo — ver instrucciones en
   `data/ai/ModelPaths.kt`. Nunca commitear este archivo (`.gitignore` ya lo excluye).
3. BLE real con Nordic Android-BLE-Library sobre el ESP32+MPU6050 (`BleSensorRepository`).
4. Fuentes custom (Space Grotesk/Inter Tight, Inter, JetBrains Mono) si hay tiempo.
5. Historial local (F7, Room) — opcional, no bloquea el MVP.
6. Ícono de launcher definitivo (hoy es un placeholder de onda/sismógrafo).

## Convenciones de commit

Conventional Commits con scope por capa/feature: `feat(structural)`, `fix(ai)`,
`refactor(domain)`, `chore(gradle)`, `docs`, `test`. Ver ejemplos y formato completo en
el `CLAUDE.md` raíz (`../CLAUDE.md`).

Antes de commitear, verificar que **no** se cuela:
- `local.properties`, `.idea/`, `build/`, `.gradle/` (deben estar ignorados — revisar
  `git status` si algo de esto aparece como nuevo/modificado, probablemente falta en
  `.gitignore` o se agregó con `git add -A` sin revisar).
- El archivo `.litertlm` del modelo (~0.8GB).
- Cualquier keystore o credencial de firma.

## Checklist antes de abrir PR (lo verificable sin hardware/emulador)

Basado en `PERQA_AGENT_BUILD_SPEC.md` §9 y `PERQA_AGENT_RULES_GEMMA_SPEED.md` §12:

- [ ] `domain/**` sigue sin imports de Android/Compose (`grep -r "android" domain/` no
      debería dar resultados salvo comentarios).
- [ ] Ningún import de `Retrofit`/`OkHttp`/`java.net` en `data/ai` ni `domain`.
- [ ] `AndroidManifest.xml` sigue sin `android.permission.INTERNET`.
- [ ] ViewModels nuevos exponen solo `StateFlow`, no `LiveData` ni estado mutable público.
- [ ] Ningún prompt nuevo incluye el token `<|think|>`.
- [ ] Toda inferencia nueva corre en `Dispatchers.Default`, nunca en Main.
- [ ] Tap targets de acciones primarias ≥56dp; el semáforo lleva color+palabra+ícono+haptic.
- [ ] Colores semánticos de veredicto sin tocar (§6.2) — si un PR los cambia, justificar por qué.
- [ ] Si el PR toca `data/sensor` o `data/ai`: sigue compilando con el stub/mock actual
      sin romper el resto de la app (nadie debería quedar bloqueado por hardware/modelo
      ausente).

Lo que **no** se puede validar sin Android Studio (compilar/correr y confirmar antes de
marcar una feature como "lista" en el PR): sync de Gradle exitoso, TalkBack, font scale,
contraste real, haptics en dispositivo, cámara real, modo avión de punta a punta.
