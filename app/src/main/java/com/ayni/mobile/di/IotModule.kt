package com.ayni.mobile.di

import android.content.Context
import androidx.room.Room
import com.ayni.mobile.data.iot.ble.BleGateway
import com.ayni.mobile.data.iot.device.NodeClientFactory
import com.ayni.mobile.data.iot.local.IotDatabase
import com.ayni.mobile.data.iot.local.StructuralStateDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DI del subsistema IoT (Room + cliente de nodo).
 *
 * Reemplaza el `AppContainer.createNodeClient` manual de ProtoEstados por Hilt, que
 * es lo que ayni ya usa para todo lo demás. Para trabajar sin hardware, cambia la
 * fábrica de abajo a `NodeClientFactory { FakeNodeClient(it) }`.
 */
@Module
@InstallIn(SingletonComponent::class)
object IotModule {

    @Provides
    @Singleton
    fun provideIotDatabase(@ApplicationContext context: Context): IotDatabase =
        Room.databaseBuilder(
            context,
            IotDatabase::class.java,
            IotDatabase.DATABASE_NAME,
        ).build()

    @Provides
    fun provideStructuralStateDao(database: IotDatabase): StructuralStateDao =
        database.structuralStateDao()

    @Provides
    @Singleton
    fun provideNodeClientFactory(
        @ApplicationContext context: Context,
    ): NodeClientFactory =
        NodeClientFactory { listener -> BleGateway(context, listener) }
}
