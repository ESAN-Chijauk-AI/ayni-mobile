package com.ayni.mobile.ui.navigation

/**
 * Rutas de navegación. Estructural y médico se agrupan en sub-grafos (structural_graph /
 * medical_graph) para compartir un único ViewModel entre captura/input y resultado
 * (hiltViewModel scoped al backstack entry del grafo padre) — así el resultado no se
 * pierde al navegar ni hace falta serializar el StructuralResult/MedicalResult a argumentos.
 */
object AyniDestinations {
    const val DISCLAIMER = "disclaimer"
    const val HOME = "home"

    const val STRUCTURAL_GRAPH = "structural_graph"
    const val STRUCTURAL_CAPTURE = "structural_capture"
    const val STRUCTURAL_RESULT = "structural_result"

    const val MEDICAL_GRAPH = "medical_graph"
    const val MEDICAL_INPUT = "medical_input"
    const val MEDICAL_RESULT = "medical_result"

    const val SENSOR_STATUS = "sensor_status"
    const val PROXIMITY = "proximity"

    // Monitoreo estructural del nodo ESP32 (subsistema IoT migrado de ProtoEstados).
    const val MONITORING = "monitoring"
}
