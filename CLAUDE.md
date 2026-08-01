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

## Subsistema IoT — reglas de no-colisión (leer antes de tocar sensores, Room o BLE)

El nodo ESP32+MPU6050 se migró completo desde `../../Hackathon-Julio2026/ProtoEstados`
y vive **autocontenido bajo el sufijo `iot/`** en cada capa. Convive con la app base
sin fusionarse. Un PR nuevo choca con esto si no respeta estas fronteras:

- **Hay DOS stacks de sensor, a propósito. No los mezcles sin una unificación deliberada.**
  1. `domain/repository/SensorRepository` + `data/sensor/{Mock,Ble}SensorRepository` — stream
     de aceleración simple para el *readout* del triage estructural (F4). Sigue siendo el
     que consume `AnalyzeStructureUseCase`/`SensorStatusViewModel`.
  2. `data/iot/device/SensorNodeClient` (+ `FakeNodeClient`) + `ui/iot/MonitoringViewModel` —
     el nodo ESP32 completo (fases REST/HITS/SEISMIC, snapshots, trazas, sismos, WiFi).
     Es el que expone `StructuralSnapshot`.
  Si implementas BLE real para el readout de triage, **puentea desde el stack IoT**
  (no dupliques escaneo/GATT en `BleSensorRepository`).
- **SOS/proximidad es un flujo de radio independiente, no un tercer stack de sensor.**
  Vive en `domain/data/ui/proximity`, anuncia y busca teléfonos Ayni, y no debe incorporar
  el protocolo ESP32 de `BleGateway`. RSSI sólo produce intensidad+tendencia, nunca metros.
- **Room ya existe: `data/iot/local/IotDatabase` (versión 1, 13 tablas).** Si añades F7
  (historial local) u otra persistencia: o agregas entidades a `IotDatabase` (sube la
  versión **y** escribe la migración), o creas otra `RoomDatabase` de forma explícita.
  Nunca resetees el esquema ni crees una segunda BD por accidente.
- **DI del nodo en `di/IotModule`** (Hilt). El cliente es intercambiable ahí
  (`BleGateway` ↔ `FakeNodeClient`, vía `NodeClientFactory`) — mismo patrón que
  `di/SensorModule` para el mock. Mantenlos separados.
- **Componentes Compose compartidos del IoT viven en `ui/iot/components/`** (SectionCard,
  StatusPill, MetricRow, LevelBar, MeasurementTraceView…). **No** los muevas a
  `ui/components/` (los de la app base): hay nombres que colisionarían.
- **`domain/iot/` es puro** igual que `domain/`. Los mappers a entidades Room están en
  `data/iot/local/IotMappers.kt`, no en el dominio.
- **`MaterialTheme.colorScheme.tertiary` ahora significa ÁMBAR** (`Amarillo`): es el color
  del invariante `FREQUENCY_SHIFT` (cambio estructural válido, no error). No lo repropongas
  para otra cosa. Los colores de veredicto (Verde/Amarillo/Rojo, §6.2) siguen intocables.
- **Ruta de navegación `AyniDestinations.MONITORING`** ya está tomada. Permisos BLE
  (`BLUETOOTH_SCAN/CONNECT`, más legacy acotados por `maxSdkVersion` en API<31) ya están
  en el manifest. Sigue **sin `INTERNET`** (el WiFi del ESP32 se configura por BLE, no por red).
- **`BleGateway` usa `BluetoothGatt` crudo con rutas compat API 26+.** No lo reescribas
  sobre Nordic sin justificarlo; el catálogo declara `nordic-ble` pero el nodo no lo usa.

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
- ~~Theme dark-first~~ **reemplazado** (ver rediseño Stitch abajo — ya no es dark-first).
- Flujos completos: Home (F1), Triage Estructural con CameraX (F2), Triage Médico (F3),
  Resultado con semáforo+haptics (F5), estado de sensor (F4 mock), disclaimer/onboarding (§7).
- **Motor Gemma real** conectado vía MediaPipe LLM Inference (`GemmaEngineImpl` ya NO es
  stub — usa `LlmInference`/`LlmInferenceSession` reales, fallback GPU→CPU, soporte imagen).
  Logs de timing con tag `GemmaEngine`/`GemmaAiRepository` para diagnosticar latencia.
- Selector de modelo in-app en Home (Storage Access Framework): el usuario elige el
  `.litertlm` desde el teléfono sin adb/PC, se copia al storage privado de la app.

**Subsistema IoT migrado desde ProtoEstados (Hackathon-Julio2026)** — port completo del
nodo ESP32+MPU6050, bajo el árbol `iot/` para no chocar con el `SensorRepository` mínimo:
- `domain/iot/`: modelos puros (StructuralSnapshot, OperationalStatus, MeasurementTrace,
  SeismicEventRecord…) + reglas puras (RobustStatistics, WifiCredentials, HitDiagnostic,
  FirmwareCapabilities, Formatting). Sin android/Room; los `asEntity` se movieron a data.
- `data/iot/ble/`: transporte real sobre `BluetoothGatt` crudo (BleGateway compat API 26+,
  NotificationAssembler, BleJsonParser, BleProtocol). No usa Nordic.
- `data/iot/device/`: `SensorNodeClient` + `FakeNodeClient` + `NodeClientFactory`.
- `data/iot/local/`: Room **esquema fresco v1** (13 tablas, DAO 800+ líneas, mappers). Sin
  historial de migraciones ni tabla médica.
- `data/iot/StructuralStateRepository.kt` (Hilt @Singleton).
- `di/IotModule.kt`: provee IotDatabase/DAO/NodeClientFactory (reemplaza el AppContainer manual).
- `ui/iot/`: `MonitoringViewModel` (@HiltViewModel, era MainViewModel), `MonitoringScreen`
  (3 pestañas: Medir/Historial/Equipo), `NodeWifiCard`, `MonitoringRoute` (permisos BLE).
  Componentes en `ui/iot/components/` (SectionCard, MeasurementTraceView, SensorOrientationView…).
- Nav: destinos `MONITORING` y `PROXIMITY` accesibles desde Herramientas (ver rediseño
  abajo — ya no cuelgan directo de Home). Manifest con permisos BLE por rango de SDK.
- SOS/proximidad (`ui/proximity/`, `data/proximity/`): advertising BLE por UUID Ayni,
  servicio foreground `connectedDevice`, detector filtrado por UUID Ayni, filtro RSSI,
  selección de peer mediante huella local de sesión y guía visual/háptica cualitativa.
  Requiere validación entre dos teléfonos reales — sin verificar en dispositivo todavía.
- `MaterialTheme.colorScheme.tertiary` = ámbar (`Amarillo`) — ver "reglas de no-colisión"
  arriba, ya corregido tras el rediseño Stitch (que trae su propio verde de marca separado).

**Rediseño visual completo sobre Stitch** (`../stitch_remix_of_ayni_mobile_emergency_response`,
ver `ayni/DESIGN.md` ahí) — reemplaza el theme dark-first original por un sistema claro
"Honey Amber" y reestructura la navegación de "elegir modo" a un modelo tipo SOS/rescate:
- `ui/theme/*`: `lightColorScheme` cálido (fondo `#FFF8F0`, primary `#7D5800`/`#F4B740`),
  tipografía Inter/JetBrains Mono (aún fuentes de sistema, mismo `TODO(fonts)`), shapes
  "hyper-rounded" (cards 24dp). `AyniSemanticColors` (verde/amarillo/rojo/negro) **sin
  tocar** — siguen siendo los hex del spec original, es la única semántica intocable.
- `ui/components/AyniBottomNav.kt`: barra flotante de 4 tabs (SOS/Herramientas/Inspección/
  Reportes), overlay sobre un `Box` en `AyniNavHost` (no `Scaffold`), visible solo en esos
  4 destinos top-level (`routeToTab()`).
- `ui/home/HomeScreen.kt`: ahora es la pantalla SOS (antes elegía ESTRUCTURA/MÉDICO). Botón
  SOS circular con hold real de 3s (`Modifier.pointerInput` + `awaitEachGesture`) que abre
  el marcador al 911 (`Intent.ACTION_DIAL`, sin permiso). "Estoy Atrapado"/"Enviar Ubicación"
  y el contacto "ICE" son **UI sin backend** (`Toast` "no implementado") — a propósito, no
  hay servicio de ubicación/SMS hoy. El selector de modelo (SAF) se mantiene igual.
- `ui/tools/ToolsScreen.kt` (nueva): grid Linterna/Señal Sonora/Brújula (placeholders
  visuales) + "Primeros Auxilios" (**real** — entra a `MEDICAL_GRAPH`) + accesos a
  "Estado del sensor" y "Monitoreo" (ambos reales, movidos aquí desde Home).
  ESTRUCTURA/MÉDICO como conceptos de "modo" ya no existen en Home; se llega a Inspección
  vía bottom nav y a Médico vía esta tarjeta.
- `ui/structural/StructuralCaptureScreen.kt`: overlay oscuro sobre el preview real de
  CameraX (sin cambios en la lógica de captura), retícula + badge "AI SENSOR RUNNING" +
  control segmentado Leve/Moderada/Riesgo Alto (**visual-only**, no hay clasificación de
  severidad propia — el veredicto real sigue viniendo solo de Gemma tras analizar).
- `ui/structural/StructuralResultScreen.kt` + `ReportsScreen.kt` (nueva) comparten
  `StructuralReportContent` (composable público reutilizable): header con badge de riesgo
  (colores semánticos intocables), foto real capturada (`StructuralViewModel.capturedImage`),
  tarjetas de fecha/hora real y GPS (**honesto**: "No disponible", no hay ubicación
  integrada), "Pulso Estructural" (reusa `SensorSignatureReadout` real), observaciones
  desde `razon`/`accion` reales de Gemma, "Compartir Reporte" real (`Intent.ACTION_SEND`).
  `data/local/LastStructuralReportState.kt` (Hilt singleton, en memoria) guarda el último
  reporte para que la pestaña "Reportes" del bottom nav lo muestre sin pasar por el flujo
  de captura — vacío hasta la primera inspección de la sesión, no es F7.
- **Sin verificar en Android Studio con dispositivo** (solo `assembleDebug` desde CLI).

Pendiente (en orden de impacto):
1. Colocar el modelo `gemma-4-E2B-it.litertlm` en el dispositivo — ver instrucciones en
   `data/ai/ModelPaths.kt` o usar el selector in-app desde Home. Nunca commitear este
   archivo (`.gitignore` ya lo excluye).
2. Migrar `GemmaEngineImpl` de MediaPipe a LiteRT-LM cuando su artefacto Maven público
   esté confirmado (hoy MediaPipe `tasks-genai`/`tasks-vision` 0.10.35, funcional pero
   marcado deprecated en Java por el propio SDK).
3. IoT: verificar compilación/Room/BLE en dispositivo; opcionalmente unificar el
   `SensorRepository` mínimo (readout de triage estructural) con el `SensorNodeClient` del
   nodo, y cablear el request de `ACCESS_FINE_LOCATION` en API<31 dentro de `MonitoringRoute`.
4. Implementar de verdad Linterna/Señal Sonora/Brújula en Herramientas si hay tiempo
   (hoy son placeholders visuales a propósito).
4. Fuentes custom (Space Grotesk/Inter Tight, Inter, JetBrains Mono) si hay tiempo.
5. Ícono de launcher definitivo (hoy es un placeholder de onda/sismógrafo).

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

- [ ] `domain/**` sigue sin imports de Android/Compose (`grep -rn "android" domain/` no
      debería dar resultados salvo comentarios) — incluye `domain/iot/**`.
- [ ] Ningún import de `Retrofit`/`OkHttp`/`java.net` en `data/ai` ni `domain`.
- [ ] `AndroidManifest.xml` sigue sin `android.permission.INTERNET`.
- [ ] IoT: no se creó un segundo stack de sensor ni una segunda `RoomDatabase`; si se tocó
      el esquema de `IotDatabase`, subió la versión **y** trae migración. Componentes IoT
      siguen en `ui/iot/components/`, no en `ui/components/`. `tertiary` sigue siendo ámbar.
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
