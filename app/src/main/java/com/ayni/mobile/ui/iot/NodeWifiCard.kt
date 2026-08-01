package com.ayni.mobile.ui.iot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.ayni.mobile.R
import com.ayni.mobile.domain.iot.NodeWifiStatus
import com.ayni.mobile.domain.iot.validateWifiCredentials
import com.ayni.mobile.ui.iot.components.SectionCard
import com.ayni.mobile.ui.iot.components.StatusPill

/**
 * Red del nodo: estado actual y cambio.
 *
 * El estado viene del propio nodo (grupo `wifi` del estado operativo) y no de
 * lo que se envió: así se ve si el cambio funcionó de verdad. Con firmware
 * anterior a 0.8 el grupo no llega y se dice, en vez de fingir «sin conexión».
 */
@Composable
internal fun NodeWifiCard(
    wifi: NodeWifiStatus?,
    enabled: Boolean,
    onSend: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var ssid by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val valid = validateWifiCredentials(ssid.trim(), password) == null

    SectionCard(
        title = stringResource(R.string.wifi_title),
        subtitle = stringResource(R.string.wifi_subtitle),
        modifier = modifier,
        accent = if (wifi?.connected == true) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.tertiary
        },
        trailing = {
            if (wifi != null) {
                StatusPill(
                    text = if (wifi.connected) {
                        stringResource(R.string.monitoring_connection_status_online)
                    } else {
                        stringResource(R.string.monitoring_connection_status_offline)
                    },
                    container = if (wifi.connected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    content = if (wifi.connected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        },
    ) {
        when {
            wifi == null -> Text(
                stringResource(R.string.wifi_unsupported),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
            )

            wifi.connected -> {
                Text(stringResource(R.string.wifi_connected, wifi.ssid))
                if (wifi.ipAddress.isNotBlank()) {
                    Text(
                        stringResource(R.string.wifi_connected_ip, wifi.ipAddress),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            else -> {
                Text(stringResource(R.string.wifi_offline, wifi.ssid))
                Text(
                    stringResource(R.string.wifi_offline_detail),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        OutlinedTextField(
            value = ssid,
            onValueChange = { ssid = it },
            label = { Text(stringResource(R.string.wifi_ssid_label)) },
            singleLine = true,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.wifi_password_label)) },
            supportingText = { Text(stringResource(R.string.wifi_password_helper)) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            stringResource(R.string.wifi_warning),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiary,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(
                onClick = {
                    onSend(ssid.trim(), password)
                    password = ""
                },
                enabled = enabled && valid,
            ) {
                Text(stringResource(R.string.wifi_send))
            }
        }
    }
}
