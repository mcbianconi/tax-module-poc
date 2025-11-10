package br.com.quintoandar.tax.core.tax.calculator

import br.com.quintoandar.tax.core.ItemSlugs
import br.com.quintoandar.tax.core.order.Order
import br.com.quintoandar.tax.core.tax.ActorTaxInfo
import br.com.quintoandar.tax.core.tax.Document
import br.com.quintoandar.tax.core.tax.PersonType
import br.com.quintoandar.tax.core.tax.Residency
import br.com.quintoandar.tax.core.tax.TaxCalculatorRegistry
import br.com.quintoandar.tax.core.tax.TaxRegime
import br.com.quintoandar.tax.core.tax.TaxType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest(classes = [br.com.quintoandar.tax.containers.api.TaxApi::class, PccBitemporalityTest.TestCalculatorsConfig::class])
class PccBitemporalityTest {
    /**
     * Define calculadores de imposto "antigas" para validar a bitemporalidade
     * no caso de uma order "do passado".
     */
    @TestConfiguration
    class TestCalculatorsConfig {
        @Bean
        fun pis2020() =
            object : TaxCalculator {
                override val taxType = TaxType.PIS
                override val rate = 0.10
                override val threshold = 1.00
                override val applicableItems = setOf(ItemSlugs.XPTO)
                override val validFrom = LocalDate.of(2020, 1, 1)
                override val validUntil = LocalDate.of(2024, 12, 31)
                override val recordedAt = LocalDateTime.of(2020, 1, 1, 0, 0)

                override fun isApplicable(
                    item: Order.Item,
                    actorTaxInfo: ActorTaxInfo,
                ) = item.slug in applicableItems
            }

        @Bean
        fun cofins2020() =
            object : TaxCalculator {
                override val taxType = TaxType.COFINS
                override val rate = 0.005
                override val threshold = 0.10
                override val applicableItems = setOf(ItemSlugs.XPTO)
                override val validFrom = LocalDate.of(2020, 1, 1)
                override val validUntil = LocalDate.of(2024, 12, 31)
                override val recordedAt = LocalDateTime.of(2020, 1, 1, 0, 0)

                override fun isApplicable(
                    item: Order.Item,
                    actorTaxInfo: ActorTaxInfo,
                ) = item.slug in applicableItems
            }

        @Bean
        fun csll2020() =
            object : TaxCalculator {
                override val taxType = TaxType.CSLL
                override val rate = 0.005
                override val threshold = 0.10
                override val applicableItems = setOf(ItemSlugs.XPTO)
                override val validFrom = LocalDate.of(2020, 1, 1)
                override val validUntil = LocalDate.of(2024, 12, 31)
                override val recordedAt = LocalDateTime.of(2020, 1, 1, 0, 0)

                override fun isApplicable(
                    item: Order.Item,
                    actorTaxInfo: ActorTaxInfo,
                ) = item.slug in applicableItems
            }

        @Bean
        @Primary
        fun testRegistry(calculators: List<TaxCalculator>) = TaxCalculatorRegistry(calculators)
    }

    @Autowired
    private lateinit var registry: TaxCalculatorRegistry

    @Autowired
    private lateinit var pccCalculator: Pcc2025

    @Test
    fun `deve usar calculadora correta para pedido de 2023`() {
        val item =
            Order.Item(
                slug = ItemSlugs.XPTO,
                value = 1000.00,
                actorId = "actor-2",
            )

        val actorTaxInfo =
            ActorTaxInfo(
                actorId = "actor-2",
                document = Document.CNPJ("12.345.678/0001-99"),
                personType = PersonType.JURIDICAL,
                residency = Residency.NATIONAL,
                taxRegime = TaxRegime.LUCRO_PRESUMIDO,
                validFrom = LocalDate.of(2020, 1, 1),
                validUntil = null,
                recordedAt = LocalDateTime.of(2020, 1, 1, 0, 0),
            )

        val orderDate = LocalDate.of(2023, 6, 15)
        val calculationDate = LocalDateTime.of(2025, 11, 10, 10, 0)

        val result =
            pccCalculator.calculate(
                item = item,
                actorTaxInfo = actorTaxInfo,
                orderDate = orderDate,
                calculationDate = calculationDate,
            )

        require(result is TaxCalculationResult.Applicable)

        val pisFromRegistry = registry.findCalculator(TaxType.PIS, orderDate, calculationDate)
        assertThat(pisFromRegistry?.rate).isEqualTo(0.10)

        val expected = (1000.00 * 0.10) + (1000.00 * 0.005) + (1000.00 * 0.005)

        assertThat(result.amount)
            .withFailMessage(
                """
                VIOLAÇÃO DE BITEMPORALIDADE

                Pedido: $orderDate | Cálculo: $calculationDate
                Registry retorna: PIS com rate=${pisFromRegistry?.rate} (Pis2020)
                Calculado: ${result.amount}
                Esperado: $expected
                """.trimIndent(),
            ).isEqualTo(expected)
    }

    @Test
    fun `deve usar calculadora correta para pedido de 2025`() {
        val item =
            Order.Item(
                slug = ItemSlugs.XPTO,
                value = 1000.00,
                actorId = "actor-2",
            )

        val actorTaxInfo =
            ActorTaxInfo(
                actorId = "actor-2",
                document = Document.CNPJ("12.345.678/0001-99"),
                personType = PersonType.JURIDICAL,
                residency = Residency.NATIONAL,
                taxRegime = TaxRegime.LUCRO_PRESUMIDO,
                validFrom = LocalDate.of(2020, 1, 1),
                validUntil = null,
                recordedAt = LocalDateTime.of(2020, 1, 1, 0, 0),
            )

        val orderDate = LocalDate.of(2025, 6, 15)
        val calculationDate = LocalDateTime.of(2025, 11, 10, 10, 0)

        val result =
            pccCalculator.calculate(
                item = item,
                actorTaxInfo = actorTaxInfo,
                orderDate = orderDate,
                calculationDate = calculationDate,
            )

        require(result is TaxCalculationResult.Applicable)

        val pisFromRegistry = registry.findCalculator(TaxType.PIS, orderDate, calculationDate)
        assertThat(pisFromRegistry?.rate).isEqualTo(0.06)

        val expected = (1000.00 * 0.06) + (1000.00 * 0.005) + (1000.00 * 0.005)
        assertThat(result.amount).isEqualTo(expected)
    }
}
