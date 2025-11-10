package br.com.quintoandar.tax.core.tax

import br.com.quintoandar.tax.core.tax.calculator.TaxCalculator
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class TaxCalculatorRegistry(
    allCalculators: List<TaxCalculator>,
) {
    private val calculatorsByType: Map<TaxType, List<TaxCalculator>> =
        allCalculators.groupBy { it.taxType }

    fun findCalculator(
        taxType: TaxType,
        validAt: LocalDate,
        knownAt: LocalDateTime,
    ): TaxCalculator? =
        calculatorsByType[taxType]
            ?.filter { it.isValidInBitemporalPeriod(validAt, knownAt) }
            ?.maxByOrNull { it.recordedAt }
}
