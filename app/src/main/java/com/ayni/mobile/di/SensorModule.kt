package com.ayni.mobile.di

import com.ayni.mobile.data.sensor.MockSensorRepository
import com.ayni.mobile.domain.repository.SensorRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Bindeado a MockSensorRepository mientras F4 (BLE real con el ESP32+MPU6050) está
 * fuera de alcance. Para activar el hardware real: cambiar el tipo de `impl` abajo a
 * BleSensorRepository (data/sensor/BleSensorRepository.kt) — ningún ViewModel ni caso
 * de uso necesita tocarse, ambos hablan solo la interfaz SensorRepository.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SensorModule {

    @Binds
    @Singleton
    abstract fun bindSensorRepository(impl: MockSensorRepository): SensorRepository
}
