package com.ayni.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.ayni.mobile.ui.home.HomeScreen
import com.ayni.mobile.ui.medical.MedicalInputScreen
import com.ayni.mobile.ui.medical.MedicalResultScreen
import com.ayni.mobile.ui.onboarding.DisclaimerScreen
import com.ayni.mobile.ui.onboarding.DisclaimerViewModel
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
    val startDestination = if (disclaimerViewModel.initiallyAcknowledged) {
        AyniDestinations.HOME
    } else {
        AyniDestinations.DISCLAIMER
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
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

        composable(AyniDestinations.HOME) {
            HomeScreen(
                onEstructuralClick = { navController.navigate(AyniDestinations.STRUCTURAL_GRAPH) },
                onMedicoClick = { navController.navigate(AyniDestinations.MEDICAL_GRAPH) },
                onSensorStatusClick = { navController.navigate(AyniDestinations.SENSOR_STATUS) },
                onDisclaimerClick = { navController.navigate(AyniDestinations.DISCLAIMER) }
            )
        }

        // Estructural (F2/F5): StructuralViewModel se scopea al backstack entry de
        // STRUCTURAL_GRAPH, compartido entre captura y resultado (mismo análisis).
        navigation(
            startDestination = AyniDestinations.STRUCTURAL_CAPTURE,
            route = AyniDestinations.STRUCTURAL_GRAPH
        ) {
            composable(AyniDestinations.STRUCTURAL_CAPTURE) { entry ->
                val parentEntry = navController.getBackStackEntry(AyniDestinations.STRUCTURAL_GRAPH)
                StructuralCaptureScreen(
                    parentEntry = parentEntry,
                    onResultReady = {
                        navController.navigate(AyniDestinations.STRUCTURAL_RESULT)
                    }
                )
            }
            composable(AyniDestinations.STRUCTURAL_RESULT) { entry ->
                val parentEntry = navController.getBackStackEntry(AyniDestinations.STRUCTURAL_GRAPH)
                StructuralResultScreen(
                    parentEntry = parentEntry,
                    onNewAnalysis = {
                        navController.popBackStack(AyniDestinations.HOME, inclusive = false)
                    },
                    onDisclaimerClick = { navController.navigate(AyniDestinations.DISCLAIMER) }
                )
            }
        }

        // Médico (F3): mismo patrón de ViewModel scopeado al sub-grafo.
        navigation(
            startDestination = AyniDestinations.MEDICAL_INPUT,
            route = AyniDestinations.MEDICAL_GRAPH
        ) {
            composable(AyniDestinations.MEDICAL_INPUT) {
                val parentEntry = navController.getBackStackEntry(AyniDestinations.MEDICAL_GRAPH)
                MedicalInputScreen(
                    parentEntry = parentEntry,
                    onResultReady = {
                        navController.navigate(AyniDestinations.MEDICAL_RESULT)
                    }
                )
            }
            composable(AyniDestinations.MEDICAL_RESULT) {
                val parentEntry = navController.getBackStackEntry(AyniDestinations.MEDICAL_GRAPH)
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
}
