package com.ayni.mobile.ui.permissions

import android.Manifest
import android.os.Build

/**
 * Permisos de runtime compartidos por cualquier flujo que escanee BLE.
 *
 * Ayni no puede declarar `neverForLocation`: Proximidad interpreta RSSI como cercanía.
 * Por ello la ubicación precisa sigue siendo necesaria para recibir resultados, además
 * de Nearby devices desde Android 12. En Android 12+ COARSE y FINE se solicitan juntas
 * para que el diálogo del sistema pueda conceder ubicación precisa correctamente.
 */
internal fun requiredBleScanPermissions(
    sdkInt: Int = Build.VERSION.SDK_INT,
): Array<String> = buildList {
    if (sdkInt >= Build.VERSION_CODES.S) {
        add(Manifest.permission.BLUETOOTH_SCAN)
        add(Manifest.permission.BLUETOOTH_CONNECT)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
    add(Manifest.permission.ACCESS_FINE_LOCATION)
}.toTypedArray()
