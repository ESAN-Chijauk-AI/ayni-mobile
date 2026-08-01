package com.ayni.mobile.data.proximity

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.ayni.mobile.domain.proximity.EmergencyProximityRepository
import com.ayni.mobile.domain.proximity.NearbySosSignal
import com.ayni.mobile.domain.proximity.ProximityScanStatus
import com.ayni.mobile.domain.proximity.SosModeStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidEmergencyProximityRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val scanner: BleSosScanner,
    private val sosStateStore: SosStateStore,
) : EmergencyProximityRepository {

    override val sosStatus: StateFlow<SosModeStatus> = sosStateStore.status
    override val scanStatus: StateFlow<ProximityScanStatus> = scanner.status
    override val nearbySignals: StateFlow<List<NearbySosSignal>> = scanner.signals

    override fun activateSos() {
        scanner.stop()
        sosStateStore.update(SosModeStatus.STARTING)
        val intent = Intent(context, EmergencyProximityService::class.java)
            .setAction(EmergencyProximityService.ACTION_START)
        runCatching { ContextCompat.startForegroundService(context, intent) }
            .onFailure { sosStateStore.update(SosModeStatus.ERROR) }
    }

    override fun deactivateSos() {
        context.stopService(Intent(context, EmergencyProximityService::class.java))
        sosStateStore.update(SosModeStatus.INACTIVE)
    }

    override fun startDetection() {
        if (sosStateStore.status.value == SosModeStatus.INACTIVE) scanner.start()
    }

    override fun stopDetection() {
        scanner.stop()
    }
}

