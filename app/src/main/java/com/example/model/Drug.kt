package com.example.model

data class Drug(
    val id: Int = 0,
    val scientificName: String,
    val category: String,
    val definition: String,
    val mechanism: String,
    val uses: String,
    val dosageGeneral: String,
    val dosageForms: String,
    val sideEffects: String,
    val contraindications: String,
    val interactions: String,
    val administration: String,
    val precautions: String,
    
    // Helper parameters for tactical dosage computation
    val weightBased: String, // "نعم" | "لا"
    val dosePerKg: String,   // E.g. "15.0" or "30-50" mg/kg
    val maxDailyDose: String, // E.g. "4000 mg" or "4g"
    val ageDependent: String, // "نعم" | "لا"
    val ageFormula: String    // Textual or mathematical guideline
)
