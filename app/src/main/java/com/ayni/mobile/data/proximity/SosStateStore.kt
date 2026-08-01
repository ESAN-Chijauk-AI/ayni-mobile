package com.ayni.mobile.data.proximity

import com.ayni.mobile.domain.proximity.SosModeStatus
import com.ayni.mobile.domain.proximity.SosReceptionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SosStateStore @Inject constructor() {
    private val mutableStatus = MutableStateFlow(SosModeStatus.INACTIVE)
    private val confirmedDevices = linkedSetOf<String>()
    private val mutableReceptionState = MutableStateFlow(SosReceptionState())
    val status: StateFlow<SosModeStatus> = mutableStatus.asStateFlow()
    val receptionState: StateFlow<SosReceptionState> = mutableReceptionState.asStateFlow()

    fun update(status: SosModeStatus) {
        mutableStatus.value = status
    }

    @Synchronized
    fun resetSession(uwbAvailable: Boolean) {
        confirmedDevices.clear()
        mutableReceptionState.value = SosReceptionState(uwbAvailable = uwbAvailable)
    }

    @Synchronized
    fun setUwbAvailable(available: Boolean) {
        mutableReceptionState.value = mutableReceptionState.value.copy(uwbAvailable = available)
    }

    @Synchronized
    fun confirmDetector(deviceAddress: String) {
        confirmedDevices += deviceAddress
        mutableReceptionState.value = mutableReceptionState.value.copy(
            confirmedDetectors = confirmedDevices.size,
        )
    }
}
