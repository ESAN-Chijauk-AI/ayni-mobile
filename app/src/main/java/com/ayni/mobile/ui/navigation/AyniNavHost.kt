package com.ayni.mobile.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.ayni.mobile.ui.components.AyniBottomNav
import com.ayni.mobile.ui.components.routeToTab
import com.ayni.mobile.ui.home.HomeScreen
import com.ayni.mobile.ui.iot.MonitoringRoute
import com.ayni.mobile.ui.medical.MedicalInputScreen
import com.ayni.mobile.ui.medical.MedicalResultScreen
import com.ayni.mobile.ui.onboarding.DisclaimerScreen
import com.ayni.mobile.ui.onboarding.DisclaimerViewModel
import com.ayni.mobile.ui.onboarding.SplashScreen
import com.ayni.mobile.ui.proximity.ProximityRoute
import com.ayni.mobile.ui.sensor.SensorStatusScreen
import com.ayni.mobile.ui.structural.StructuralCaptureScreen
import com.ayni.mobile.ui.structural.StructuralResultScreen

@Composable
fun AyniNavHost(
    navController: NavHostController = rememberNavController()
) {
    // Decisión tomada una sola vez al crear el grafo (§7: el disclaimer es onboarding
    // obligatorio la primera vez). initiallyAcknowledged es un snapshot en la
    // construcción del ViewModel, no reactivo — no necesita serlo: una vez que el
    // usuario avanza, la navegación posterior ya no depende de startDestination.
    val disclaimerViewModel: DisclaimerViewModel = hiltViewModel()
    val postSplashDestination = if (disclaimerViewModel.initiallyAcknowledged) {
        AyniDestinations.HOME
    } else {
        AyniDestinations.DISCLAIMER
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = AyniDestinations.SPLASH,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(AyniDestinations.SPLASH) {
                SplashScreen(
                    onFinished = {
                        navController.navigate(postSplashDestination) {
                            popUpTo(AyniDestinations.SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            composable(AyniDestinations.DISCLAIMER) {
                DisclaimerScreen(
                    onContinue = {
                        if (navController.previousBackStackEntry == null) {
                            navController.navigate(AyniDestinations.HOME) {
                                popUpTo(AyniDestinations.DISCLAIMER) { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }
                )
            }

            composable(AyniDestinations.SENSOR_STATUS) {
                SensorStatusScreen(onBack = { navController.popBackStack() })
            }

            // Monitoreo estructural del nodo ESP32 (subsistema IoT). MonitoringRoute
            // engancha su propio MonitoringViewModel (Hilt) y gestiona el permiso BLE.
            composable(AyniDestinations.MONITORING) {
                MonitoringRoute()
            }

            // Red de proximidad SOS por BLE (broadcast/scan de emergencia cercana).
            composable(AyniDestinations.PROXIMITY) {
                ProximityRoute(onBack = { navController.popBackStack() })
            }

            composable(AyniDestinations.HOME) {
                HomeScreen(
                    onDisclaimerClick = { navController.navigate(AyniDestinations.DISCLAIMER) },
                    onMedicalClick = { navController.navigate(AyniDestinations.MEDICAL_GRAPH) },
                    onStructuralClick = { navController.navigate(AyniDestinations.STRUCTURAL_GRAPH) { launchSingleTop = true } }
                )
            }

            // Estructural (F2/F5): StructuralViewModel se scopea al backstack entry de
            // STRUCTURAL_GRAPH, compartido entre captura y resultado (mismo análisis).
            navigation(
                startDestination = AyniDestinations.STRUCTURAL_CAPTURE,
                route = AyniDestinations.STRUCTURAL_GRAPH
            ) {
                composable(AyniDestinations.STRUCTURAL_CAPTURE) { entry ->
                    val parentEntry = remember(entry) {
                        navController.getBackStackEntry(AyniDestinations.STRUCTURAL_GRAPH)
                    }
                    StructuralCaptureScreen(
                        parentEntry = parentEntry,
                        onResultReady = {
                            navController.navigate(AyniDestinations.STRUCTURAL_RESULT)
                        },
                        onClose = { navController.popBackStack() },
                        onSensorStatusClick = { navController.navigate(AyniDestinations.SENSOR_STATUS) },
                        onMonitoringClick = { navController.navigate(AyniDestinations.MONITORING) }
                    )
                }
                composable(AyniDestinations.STRUCTURAL_RESULT) { entry ->
                    val parentEntry = remember(entry) {
                        navController.getBackStackEntry(AyniDestinations.STRUCTURAL_GRAPH)
                    }
                    StructuralResultScreen(
                        parentEntry = parentEntry,
                        onNewAnalysis = {
                            navController.popBackStack(AyniDestinations.HOME, inclusive = false)
                        },
                        onDisclaimerClick = { navController.navigate(AyniDestinations.DISCLAIMER) }
                    )
                }
            }

            // Médico (F3): mismo patrón de ViewModel scopeado al sub-grafo. Se entra
            // desde la tarjeta "Primeros Auxilios" en Herramientas.
            navigation(
                startDestination = AyniDestinations.MEDICAL_INPUT,
                route = AyniDestinations.MEDICAL_GRAPH
            ) {
                composable(AyniDestinations.MEDICAL_INPUT) { entry ->
                    val parentEntry = remember(entry) {
                        navController.getBackStackEntry(AyniDestinations.MEDICAL_GRAPH)
                    }
                    MedicalInputScreen(
                        parentEntry = parentEntry,
                        onResultReady = {
                            navController.navigate(AyniDestinations.MEDICAL_RESULT)
                        }
                    )
                }
                composable(AyniDestinations.MEDICAL_RESULT) { entry ->
                    val parentEntry = remember(entry) {
                        navController.getBackStackEntry(AyniDestinations.MEDICAL_GRAPH)
                    }
                    MedicalResultScreen(
                        parentEntry = parentEntry,
                        onNewAnalysis = {
                            navController.popBackStack(AyniDestinations.HOME, inclusive = false)
                        },
                        onDisclaimerClick = { navController.navigate(AyniDestinations.DISCLAIMER) }
                    )
                }
            }
        }

        // Bottom nav flotante: overlay, no Scaffold — solo visible en los 4 destinos
        // top-level que representa (routeToTab devuelve null en el resto, p.ej.
        // Monitoreo/Sensor Status, que se navegan "por encima").
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentTab = routeToTab(backStackEntry?.destination?.route)
        if (currentTab != null) {
            AyniBottomNav(
                selected = currentTab,
                onSosClick = {
                    navController.navigate(AyniDestinations.HOME) {
                        popUpTo(AyniDestinations.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onMedicalClick = {
                    navController.navigate(AyniDestinations.MEDICAL_GRAPH) { launchSingleTop = true }
                },
                onStructuralClick = {
                    navController.navigate(AyniDestinations.STRUCTURAL_GRAPH) { launchSingleTop = true }
                },
                onProximityClick = {
                    navController.navigate(AyniDestinations.PROXIMITY) { launchSingleTop = true }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
