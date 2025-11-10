package br.com.quintoandar.tax.core.tax.calculator

sealed interface TaxCalculationResult {
    data class Applicable(
        val amount: Double,
    ) : TaxCalculationResult

    data class NotApplicable(
        val reason: String,
    ) : TaxCalculationResult

    data class BelowThreshold(
        val calculatedAmount: Double,
        val threshold: Double,
    ) : TaxCalculationResult
}
