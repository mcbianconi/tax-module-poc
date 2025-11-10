package br.com.quintoandar.tax.containers.api

import br.com.quintoandar.tax.core.ItemSlugs.XPTO
import br.com.quintoandar.tax.core.order.Order
import br.com.quintoandar.tax.core.tax.TaxCalculatorService
import br.com.quintoandar.tax.core.tax.TaxType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest
class TaxCalculationE2ETest {
    @Autowired
    private lateinit var taxCalculatorService: TaxCalculatorService

    @Test
    fun `deve calcular PCC para servico de PJ lucro presumido`() {
        // Actor-2 do CSV: PJ Lucro Presumido
        val order =
            Order(
                id = "order-1",
                date = LocalDate.of(2025, 3, 1),
                items =
                    listOf(
                        Order.Item(
                            slug = XPTO,
                            value = 1000.00,
                            actorId = "actor-2",
                        ),
                    ),
            )

        val result =
            taxCalculatorService.calculate(
                order = order,
                calculationDate = LocalDateTime.of(2025, 3, 1, 10, 0),
            )

        // Deve ter PCC
        val itemResult = result.itemResults.first()
        val pccTax = itemResult.taxes.find { it.taxType == TaxType.PCC }

        assertThat(pccTax).isNotNull()
        // PCC = PIS (6%) + COFINS (0.5%) + CSLL (0.5%) = 70.00
        assertThat(pccTax!!.amount).isEqualTo(70.00)
    }

    @Test
    fun `deve calcular ISS para servico de simples nacional`() {
        // Actor-1 do CSV: PJ Simples Nacional
        val order =
            Order(
                id = "order-2",
                date = LocalDate.of(2025, 3, 1),
                items =
                    listOf(
                        Order.Item(
                            slug = XPTO,
                            value = 1000.00,
                            actorId = "actor-1",
                        ),
                    ),
            )

        val result = taxCalculatorService.calculate(order)

        val itemResult = result.itemResults.first()
        val issTax = itemResult.taxes.find { it.taxType == TaxType.ISS }

        assertThat(issTax).isNotNull()
        // ISS = 6% (rate 0.06)
        assertThat(issTax!!.amount).isEqualTo(60.00)
    }

    @Test
    fun `nao deve cobrar PCC se abaixo do threshold`() {
        val order =
            Order(
                id = "order-4",
                date = LocalDate.of(2025, 3, 1),
                items =
                    listOf(
                        Order.Item(
                            slug = XPTO,
                            value = 100.00,
                            actorId = "actor-2",
                        ),
                    ),
            )

        val result = taxCalculatorService.calculate(order)

        val itemResult = result.itemResults.first()
        val pccTax = itemResult.taxes.find { it.taxType == TaxType.PCC }

        // Não deve cobrar PCC pois ficou abaixo do threshold
        assertThat(pccTax).isNull()
    }

    @Test
    fun `deve calcular impostos para order com multiplos itens`() {
        val order =
            Order(
                id = "order-5",
                date = LocalDate.of(2025, 3, 1),
                items =
                    listOf(
                        Order.Item(
                            slug = XPTO,
                            value = 1000.00,
                            actorId = "actor-2", // PJ Lucro Presumido
                        ),
                        Order.Item(
                            slug = XPTO,
                            value = 500.00,
                            actorId = "actor-1", // PJ Simples Nacional
                        ),
                        Order.Item(
                            slug = XPTO,
                            value = 3000.00,
                            actorId = "actor-2", // PJ Lucro Presumido
                        ),
                    ),
            )

        val result = taxCalculatorService.calculate(order)

        assertThat(result.itemResults).hasSize(3)

        // Item 1: PCC (1000 * 7% = 70.00)
        assertThat(result.itemResults[0].taxes)
            .anyMatch { it.taxType == TaxType.PCC && it.amount == 70.00 }

        // Item 2: ISS (500 * 6% = 30.00)
        assertThat(result.itemResults[1].taxes)
            .anyMatch { it.taxType == TaxType.ISS && it.amount == 30.00 }

        // Item 3: PCC (3000 * 7% = 210.00)
        assertThat(result.itemResults[2].taxes)
            .anyMatch { it.taxType == TaxType.PCC && it.amount == 210.00 }
    }

    @Test
    fun `deve respeitar mudanca de regime tributario ao longo do tempo`() {
        // Actor-4 mudou de Lucro Real para Simples em 01/07/2025

        // Order de junho - antes da mudança
        val orderJune =
            Order(
                id = "order-6",
                date = LocalDate.of(2025, 6, 15),
                items =
                    listOf(
                        Order.Item(
                            slug = XPTO,
                            value = 2000.00,
                            actorId = "actor-4",
                        ),
                    ),
            )

        val resultJune =
            taxCalculatorService.calculate(
                order = orderJune,
                calculationDate = LocalDateTime.of(2025, 6, 15, 10, 0),
            )

        // Order de agosto - depois da mudança
        val orderAugust =
            Order(
                id = "order-7",
                date = LocalDate.of(2025, 8, 1),
                items =
                    listOf(
                        Order.Item(
                            slug = XPTO,
                            value = 2000.00,
                            actorId = "actor-4",
                        ),
                    ),
            )

        val resultAugust =
            taxCalculatorService.calculate(
                order = orderAugust,
                calculationDate = LocalDateTime.of(2025, 8, 1, 10, 0),
            )

        // Junho: deve ter PCC (Lucro Real)
        val juneItem = resultJune.itemResults.first()
        val junePcc = juneItem.taxes.find { it.taxType == TaxType.PCC }
        assertThat(junePcc).isNotNull()
        assertThat(junePcc!!.amount).isEqualTo(140.00) // 2000 * 7%

        // Agosto: deve ter ISS (Simples Nacional)
        val augustItem = resultAugust.itemResults.first()
        val augustIss = augustItem.taxes.find { it.taxType == TaxType.ISS }
        val augustPcc = augustItem.taxes.find { it.taxType == TaxType.PCC }
        assertThat(augustIss).isNotNull()
        assertThat(augustIss!!.amount).isEqualTo(120.00) // 2000 * 6%
        assertThat(augustPcc).isNull() // Não deve ter PCC no Simples
    }
}
