package com.ayni.mobile.ui.onboarding

import androidx.lifecycle.ViewModel
import com.ayni.mobile.data.local.DisclaimerAcknowledgedState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DisclaimerViewModel @Inject constructor(
    private val disclaimerAcknowledgedState: DisclaimerAcknowledgedState
) : ViewModel() {

    /** Snapshot al crear el ViewModel — usado por AyniNavHost para elegir startDestination. */
    val initiallyAcknowledged: Boolean = disclaimerAcknowledgedState.acknowledged.value

    fun onAcknowledge() {
        disclaimerAcknowledgedState.acknowledge()
    }
}
