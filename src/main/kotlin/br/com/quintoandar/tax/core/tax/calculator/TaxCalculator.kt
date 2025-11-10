package br.com.quintoandar.tax.core.tax.calculator

import br.com.quintoandar.tax.core.Bitemporal
import br.com.quintoandar.tax.core.ItemSlug
import br.com.quintoandar.tax.core.order.Order
import br.com.quintoandar.tax.core.tax.ActorTaxInfo
import br.com.quintoandar.tax.core.tax.TaxType
import java.time.LocalDate
import java.time.LocalDateTime

interface TaxCalculator : Bitemporal {
    val taxType: TaxType
    val threshold: Double
    val applicableItems: Set<ItemSlug>
    val rate: Double

    override val validFrom: LocalDate
    override val validUntil: LocalDate?
    override val recordedAt: LocalDateTime

    /**
     * Determines if this tax calculator applies to the given item and actor tax info.
     * This includes both item-level checks (e.g., item slug) and eligibility rules
     * (e.g., tax regime, person type) that may change over time.
     *
     * By implementing eligibility rules here, they become bitemporal along with the calculator.
     */
    fun isApplicable(item: Order.Item, actorTaxInfo: ActorTaxInfo): Boolean

    fun calculate(
        item: Order.Item,
        actorTaxInfo: ActorTaxInfo,
        orderDate: LocalDate,
        calculationDate: LocalDateTime
    ): TaxCalculationResult {
        if (!isApplicable(item, actorTaxInfo)) {
            return TaxCalculationResult.NotApplicable("$item not applicable to $this")
        }

        val amount = item.value * rate

        return if (amount < threshold) {
            TaxCalculationResult.BelowThreshold(amount, threshold)
        } else {
            TaxCalculationResult.Applicable(amount)
        }
    }
}
