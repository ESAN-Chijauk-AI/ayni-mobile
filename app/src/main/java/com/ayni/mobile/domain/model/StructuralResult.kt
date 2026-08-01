package com.ayni.mobile.domain.model

data class StructuralResult(
    val verdict: StructuralVerdict,
    val razon: String,
    val accion: String,
    val usoSensor: Boolean
)
