package br.com.quintoandar.tax.core.tax.calculator

import br.com.quintoandar.tax.core.ItemSlugs.XPTO
import br.com.quintoandar.tax.core.order.Order
import br.com.quintoandar.tax.core.tax.ActorTaxInfo
import br.com.quintoandar.tax.core.tax.PersonType
import br.com.quintoandar.tax.core.tax.TaxCalculatorRegistry
import br.com.quintoandar.tax.core.tax.TaxRegime
import br.com.quintoandar.tax.core.tax.TaxType
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class Pcc2025(
    @param:Lazy private val registry: TaxCalculatorRegistry,
) : TaxCalculator {
    override val taxType = TaxType.PCC
    override val rate = 0.005
    override val threshold = 10.00
    override val applicableItems = setOf(XPTO)
    override val validFrom: LocalDate = LocalDate.of(2025, 1, 1)
    override val validUntil = null
    override val recordedAt: LocalDateTime = LocalDateTime.of(2025, 1, 1, 10, 0)

    override fun isApplicable(
        item: Order.Item,
        actorTaxInfo: ActorTaxInfo,
    ): Boolean =
        item.slug in applicableItems &&
            actorTaxInfo.personType == PersonType.JURIDICAL &&
            actorTaxInfo.taxRegime != TaxRegime.SIMPLES_NACIONAL

    override fun calculate(
        item: Order.Item,
        actorTaxInfo: ActorTaxInfo,
        orderDate: LocalDate,
        calculationDate: LocalDateTime,
    ): TaxCalculationResult {
        if (!isApplicable(item, actorTaxInfo)) {
            return TaxCalculationResult.NotApplicable("$item not applicable to $this")
        }

        val pis = registry.findCalculator(TaxType.PIS, orderDate, calculationDate)
        val cofins = registry.findCalculator(TaxType.COFINS, orderDate, calculationDate)
        val csll = registry.findCalculator(TaxType.CSLL, orderDate, calculationDate)

        val componentAmounts =
            listOfNotNull(pis, cofins, csll)
                .map { it.calculate(item, actorTaxInfo, orderDate, calculationDate) }
                .filter { it is TaxCalculationResult.Applicable }
                .sumOf { (it as TaxCalculationResult.Applicable).amount }

        return if (componentAmounts < threshold) {
            TaxCalculationResult.BelowThreshold(componentAmounts, threshold)
        } else {
            TaxCalculationResult.Applicable(componentAmounts)
        }
    }
}
