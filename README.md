# Ayni — app Android nativa (Kotlin)

App móvil **offline-first** de respuesta post-sismo. Dos módulos de triage con IA local
(Gemma 4 E2B):

- **Estructural** — foto de grieta + acelerómetro (ESP32/MPU6050) → semáforo
  VERDE/AMARILLO/ROJO estilo ATC-20.
- **Médico** — lesión → prioridad START.
- **SOS de proximidad** — un teléfono emite una baliza BLE anónima y otros teléfonos
  con Ayni usan intensidad+tendencia como guía cualitativa de búsqueda, sin inventar metros.

`applicationId` / namespace: `com.ayni.mobile`. Diseño claro "Honey Amber" (rediseño
Stitch), sin permiso `INTERNET`: todo el triage corre en el dispositivo.

> La guía operativa para trabajar en este repo (reglas de arquitectura, invariantes,
> checklist de PR y estado actual) está en **[`CLAUDE.md`](CLAUDE.md)** — léela antes de
> escribir código. Las fuentes de verdad del producto están en `../documentation/`
> (`PERQA_AGENT_BUILD_SPEC.md`, `PERQA_AGENT_RULES_GEMMA_SPEED.md`, `RUBRICA.MD`).

## Requisitos

- Android Studio (JBR 17+) · `compileSdk 35` · `minSdk 26` · Gradle Kotlin DSL + Version Catalog.
- Hilt (DI), Jetpack Compose + Material3, Navigation Compose, Room, CameraX, MediaPipe
  `tasks-genai`/`tasks-vision` (motor Gemma), Nordic BLE (declarado, ver más abajo).

```bash
./gradlew.bat assembleDebug        # APK de desarrollo
```

El modelo `gemma-4-E2B-it.litertlm` (~0.8 GB) **no** se commitea; se coloca en el
dispositivo aparte (ver `data/ai/ModelPaths.kt`).

## Estructura (capas unidireccionales UI → domain → data → di)

Todo bajo `app/src/main/java/com/ayni/mobile/`:

```
ui/            Compose + ViewModels (@HiltViewModel, estado como StateFlow inmutable)
  home/  structural/  medical/  sensor/  onboarding/  components/  navigation/  theme/
  iot/         ← sección de MONITOREO del nodo ESP32 (ver abajo)
  proximity/   ← pantalla SOS/detector BLE teléfono-a-teléfono
domain/        Kotlin PURO (sin android/androidx/Compose): modelos, repos (interfaces), use cases
  model/  repository/  usecase/
  iot/         ← modelos y reglas puras del nodo ESP32
  proximity/   ← estados cualitativos y filtro RSSI puro
data/          implementaciones: IA, sensor, persistencia
  ai/  sensor/  local/
  iot/         ← todo el subsistema IoT (BLE + Room + repositorio del nodo)
  proximity/   ← advertising, scan filtrado y foreground service SOS
di/            módulos Hilt
```

## Subsistema IoT (nodo ESP32 + MPU6050)

Migrado completo desde `../../Hackathon-Julio2026/ProtoEstados` y **autocontenido bajo el
sufijo `iot/`** en cada capa, para no chocar con el resto de la app.

| Ruta | Qué es |
|---|---|
| `domain/iot/` | Modelos puros (`StructuralSnapshot`, `OperationalStatus`, `MeasurementTrace`, `SeismicEventRecord`…) y reglas puras (`RobustStatistics`, `WifiCredentials`, `HitDiagnostic`, `FirmwareCapabilities`, `Formatting`). |
| `data/iot/ble/` | Transporte real BLE sobre `BluetoothGatt` crudo, compatible API 26+ (`BleGateway`, `NotificationAssembler`, `BleJsonParser`, `BleProtocol`). |
| `data/iot/device/` | `SensorNodeClient` (interfaz, transporte-agnóstica), `FakeNodeClient` (dev sin hardware), `NodeClientFactory`. |
| `data/iot/local/` | Room: esquema **fresco v1** (13 tablas), `StructuralStateDao`, `IotDatabase`, mappers, reglas de re-entrega. |
| `data/iot/StructuralStateRepository.kt` | API interna de datos del nodo (Hilt `@Singleton`). |
| `di/IotModule.kt` | Provee `IotDatabase`, el DAO y el `NodeClientFactory` (intercambia `BleGateway` ↔ `FakeNodeClient`). |
| `ui/iot/` | `MonitoringViewModel` (`@HiltViewModel`), `MonitoringScreen` (pestañas Medir / Historial / Equipo), `NodeWifiCard`, `MonitoringRoute`. Componentes compartidos en `ui/iot/components/`. |

Se entra al monitoreo desde **Home → Monitoreo** (`AyniDestinations.MONITORING`).

### Sin hardware ESP32

En `di/IotModule.kt`, cambia la fábrica para que devuelva el nodo simulado:

```kotlin
NodeClientFactory { listener -> FakeNodeClient(listener) }
```

### Reglas de no-colisión (importante para futuras implementaciones)

- **Coexisten dos stacks de sensor a propósito.** El `SensorRepository` mínimo
  (`data/sensor/…`, stream de aceleración para el readout del triage F4) es independiente
  del nodo ESP32 completo (`data/iot/device/SensorNodeClient` + `ui/iot/MonitoringViewModel`).
  No los fusiones sin una unificación deliberada.
- **Proximidad es un tercer flujo BLE deliberado, no otro sensor.** Vive bajo
  `*/proximity/` y no reutiliza `BleGateway`, cuyo protocolo es específico del ESP32.
  No ejecutes Monitoreo y detector/SOS simultáneamente sin coordinar la radio.
- **Room ya existe** (`data/iot/local/IotDatabase`, v1). Para nueva persistencia: extiende
  ese esquema (sube versión + migración) o crea otra `RoomDatabase` explícitamente. No
  resetees el esquema.
- **Componentes Compose del IoT** van en `ui/iot/components/`, **no** en `ui/components/`
  (hay nombres que colisionarían, p. ej. `SectionCard`).
- **`colorScheme.tertiary` = ámbar** (`Amarillo`): color del invariante `FREQUENCY_SHIFT`.
  No lo repropongas. Los colores de veredicto (Verde/Amarillo/Rojo) son intocables.
- **Sigue sin `INTERNET`**: el WiFi del ESP32 se configura por BLE (`SET_WIFI`), no por red
  del teléfono.

## Estado

Ver la sección «Estado actual» de [`CLAUDE.md`](CLAUDE.md). Lo verificable sin
Android Studio/dispositivo está en su «Checklist antes de abrir PR»; Gradle sync, BLE real,
cámara, TalkBack y contraste se validan en dispositivo.
