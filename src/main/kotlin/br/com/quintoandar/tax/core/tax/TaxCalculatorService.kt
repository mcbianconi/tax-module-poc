package br.com.quintoandar.tax.core.tax

import br.com.quintoandar.tax.core.order.ActorId
import br.com.quintoandar.tax.core.order.Order
import br.com.quintoandar.tax.core.order.OrderId
import br.com.quintoandar.tax.core.tax.calculator.TaxCalculationResult
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class TaxCalculatorService(
    private val taxInfoRepository: TaxInfoRepository,
    private val calculatorRegistry: TaxCalculatorRegistry,
) {
    fun calculate(
        order: Order,
        calculationDate: LocalDateTime = LocalDateTime.now(),
    ): OrderTaxResult {
        val actorIds = order.items.map { it.actorId }.toSet()
        val taxInfoByActor =
            taxInfoRepository.findByActorIds(
                actorIds = actorIds,
                validAt = order.date,
                knownAt = calculationDate,
            )

        val itemResults =
            order.items.map { item ->
                val taxInfo =
                    taxInfoByActor[item.actorId]
                        ?: throw ActorTaxInfoNotFoundException(item.actorId)

                calculateTaxesForItem(item, taxInfo, order.date, calculationDate)
            }

        return OrderTaxResult(order.id, itemResults)
    }

    fun calculateTaxesForItem(
        item: Order.Item,
        actorTaxInfo: ActorTaxInfo,
        orderDate: LocalDate,
        calculationDate: LocalDateTime,
    ): ItemTaxResult {
        // Try all tax types - each calculator decides if it's applicable
        // This ensures eligibility rules are bitemporal (part of the calculator version)
        val taxes =
            TaxType.entries.mapNotNull { taxType ->
                val calculator = calculatorRegistry.findCalculator(taxType, orderDate, calculationDate)

                val taxResult = calculator?.calculate(item, actorTaxInfo, orderDate, calculationDate)

                taxResult?.let { result ->
                    when (result) {
                        is TaxCalculationResult.Applicable -> ItemTax(taxType, result.amount)
                        else -> null
                    }
                }
            }

        return ItemTaxResult(item, taxes)
    }
}

class ActorTaxInfoNotFoundException(
    actorId: ActorId,
) : RuntimeException("TaxInfo não encontrado para actor: $actorId")

data class OrderTaxResult(
    val orderId: OrderId,
    val itemResults: List<ItemTaxResult>,
)
