package com.ayni.mobile.di

import com.ayni.mobile.data.proximity.AndroidEmergencyProximityRepository
import com.ayni.mobile.domain.proximity.EmergencyProximityRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProximityModule {

    @Binds
    @Singleton
    abstract fun bindEmergencyProximityRepository(
        implementation: AndroidEmergencyProximityRepository,
    ): EmergencyProximityRepository
}
