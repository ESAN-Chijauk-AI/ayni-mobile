package com.ayni.mobile.ui.permissions

import android.Manifest
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Test

class BleScanPermissionsTest {
    @Test
    fun `Android 11 solicita ubicacion precisa para descubrir BLE`() {
        assertEquals(
            listOf(Manifest.permission.ACCESS_FINE_LOCATION),
            requiredBleScanPermissions(Build.VERSION_CODES.R).toList(),
        )
    }

    @Test
    fun `Android 12 agrega Nearby devices y ambas precisiones de ubicacion`() {
        assertEquals(
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ),
            requiredBleScanPermissions(Build.VERSION_CODES.S).toList(),
        )
    }
}
