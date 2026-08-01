package com.ayni.mobile.data.proximity

import com.ayni.mobile.domain.proximity.SosModeStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SosStateStore @Inject constructor() {
    private val mutableStatus = MutableStateFlow(SosModeStatus.INACTIVE)
    val status: StateFlow<SosModeStatus> = mutableStatus.asStateFlow()

    fun update(status: SosModeStatus) {
        mutableStatus.value = status
    }
}

