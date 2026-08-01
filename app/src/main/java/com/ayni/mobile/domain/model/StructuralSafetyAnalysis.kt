package com.ayni.mobile.domain.model

data class StructuralSafetyAnalysis(
    val verdict: StructuralVerdict,
    val summary: String,
    val steps: List<String>,
)
