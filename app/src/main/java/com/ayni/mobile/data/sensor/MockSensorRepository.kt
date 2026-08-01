package com.ayni.mobile.data.sensor

import com.ayni.mobile.domain.model.SensorReading
import com.ayni.mobile.domain.repository.SensorRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Simulación del ESP32+MPU6050 mientras no hay hardware integrado (F4 despriorizado
 * para esta sesión). Emite ~12Hz con ruido bajo en reposo; triggerSimulatedAftershock()
 * sube el ruido ~3s para imitar una réplica — es lo que permite demostrar en vivo que
 * el sensor puede subir el veredicto estructural (el diferenciador del spec), sin
 * necesitar el sensor físico. Toda lectura sale marcada isSimulated = true.
 *
 * BleSensorRepository (stub) reemplazará el binding de SensorModule cuando se
 * implemente el hardware real; ningún consumidor (ViewModels, use cases) necesita
 * cambiar porque ambos hablan solo la interfaz SensorRepository.
 */
@Singleton
class MockSensorRepository @Inject constructor() : SensorRepository {

    private val _isConnected = MutableStateFlow(true) // el mock está "conectado" por definición

    override val isConnected: StateFlow<Boolean> = _isConnected

    @Volatile
    private var aftershockUntilMs: Long = 0L

    private val restNoise = 0.15f
    private val aftershockNoise = 3.5f
    private val gravity = 9.8f
    private val emissionIntervalMs = 80L // ~12Hz, suficiente para el readout visual

    override val readings: Flow<SensorReading> = flow {
        while (currentCoroutineContext().isActive) {
            val now = System.currentTimeMillis()
            val aftershockActive = now < aftershockUntilMs
            val noise = if (aftershockActive) aftershockNoise else restNoise

            val ax = jitter(noise)
            val ay = jitter(noise)
            val az = gravity + jitter(noise)
            val magnitud = sqrt(ax * ax + ay * ay + az * az)

            emit(
                SensorReading(
                    ax = ax,
                    ay = ay,
                    az = az,
                    magnitud = magnitud,
                    ts = now,
                    isSimulated = true
                )
            )
            delay(emissionIntervalMs)
        }
    }

    override fun triggerSimulatedAftershock() {
        aftershockUntilMs = System.currentTimeMillis() + 3_000L
    }

    override fun connect() {
        _isConnected.value = true
    }

    override fun disconnect() {
        _isConnected.value = false
    }

    private fun jitter(noise: Float): Float = (Random.nextFloat() - 0.5f) * noise * 2f
}
