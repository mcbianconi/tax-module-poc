package br.com.quintoandar.tax.core.tax.calculator

import br.com.quintoandar.tax.core.ItemSlugs.XPTO
import br.com.quintoandar.tax.core.order.Order
import br.com.quintoandar.tax.core.tax.ActorTaxInfo
import br.com.quintoandar.tax.core.tax.TaxType
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class Pis2018 : TaxCalculator {
    override val taxType = TaxType.PIS
    override val rate = 0.06
    override val threshold = 1.00
    override val applicableItems = setOf(XPTO)
    override val validFrom: LocalDate = LocalDate.of(2025, 1, 1)
    override val validUntil = null
    override val recordedAt: LocalDateTime = LocalDateTime.of(2025, 1, 1, 10, 0)

    // PIS is a component of PCC, so only check item applicability
    // The eligibility rules are enforced at the PCC level
    override fun isApplicable(
        item: Order.Item,
        actorTaxInfo: ActorTaxInfo,
    ): Boolean = item.slug in applicableItems
}
