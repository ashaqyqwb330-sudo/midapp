package com.example.helper

import com.example.model.Drug

object CalculatorLogic {

    /**
     * Extracts the first positive decimal number from a string.
     * E.g. "15 mg/kg" -> 15.0, "30-50" -> 30.0, "4g" -> 4.0
     */
    fun parseNumber(text: String): Double? {
        val cleaned = text.filter { it.isDigit() || it == '.' }
        return cleaned.toDoubleOrNull()
    }

    /**
     * Calculates the appropriate dosage based on drug configuration and user inputs.
     * @return CalculationResult containing details or validation error.
     */
    fun calculateDosage(
        drug: Drug,
        weightKg: Double?,
        ageYears: Double?,
        customDosePerKg: Double?,
        concentrationMgMl: Double?
    ): CalculationResult {
        if (drug.weightBased == "نعم") {
            if (weightKg == null || weightKg <= 0) {
                return CalculationResult.Error("يرجى إدخال وزن صحيح للمريض (أكبر من 0)")
            }

            val dosePerKg = customDosePerKg ?: parseNumber(drug.dosePerKg) ?: 0.0
            if (dosePerKg <= 0) {
                return CalculationResult.Error("حجم الجرعة لكل كجم غير دقيق أو مفقود")
            }

            val totalDose = weightKg * dosePerKg
            val maxDailyDoseVal = parseNumber(drug.maxDailyDose) ?: 0.0
            
            val isCapped = maxDailyDoseVal > 0.0 && totalDose > maxDailyDoseVal
            val cappedDose = if (isCapped) maxDailyDoseVal else totalDose
            val roundedDose = Math.round(cappedDose * 100.0) / 100.0

            val volumeMl = if (concentrationMgMl != null && concentrationMgMl > 0.0) {
                Math.round((roundedDose / concentrationMgMl) * 100.0) / 100.0
            } else {
                null
            }

            val summaryText = buildString {
                append("$roundedDose mg")
                if (volumeMl != null) {
                    append(" ($volumeMl mL)")
                }
                if (isCapped) {
                    append("\n⚠️ تجاوز الحد الأقصى للمستحضر! تم الخفض بالقيمة القصوى: $maxDailyDoseVal mg")
                }
            }

            return CalculationResult.Success(
                doseMg = roundedDose,
                volumeMl = volumeMl,
                isMaxCapped = isCapped,
                summary = summaryText
            )
        } else if (drug.ageDependent == "نعم") {
            if (ageYears == null || ageYears < 0) {
                return CalculationResult.Error("يرجى إدخال عمر صحيح للمريض")
            }
            return CalculationResult.Success(
                doseMg = 0.0,
                volumeMl = null,
                isMaxCapped = false,
                summary = "مبني على العمر: ${drug.dosageGeneral}\n💡 إرشادات السن: ${drug.ageFormula}"
            )
        } else {
            return CalculationResult.Success(
                doseMg = parseNumber(drug.dosageGeneral) ?: 0.0,
                volumeMl = null,
                isMaxCapped = false,
                summary = drug.dosageGeneral
            )
        }
    }
}

sealed class CalculationResult {
    data class Success(
        val doseMg: Double,
        val volumeMl: Double?,
        val isMaxCapped: Boolean,
        val summary: String
    ) : CalculationResult()

    data class Error(val message: String) : CalculationResult()
}
