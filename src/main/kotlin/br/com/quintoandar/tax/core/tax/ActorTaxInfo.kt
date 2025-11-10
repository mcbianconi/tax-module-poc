package br.com.quintoandar.tax.core.tax

import br.com.quintoandar.tax.core.Bitemporal
import br.com.quintoandar.tax.core.order.ActorId
import java.time.LocalDate
import java.time.LocalDateTime

data class ActorTaxInfo(
    val actorId: ActorId,
    val document: Document,
    val personType: PersonType,
    val residency: Residency,
    val taxRegime: TaxRegime,
    override val validFrom: LocalDate,
    override val validUntil: LocalDate?,
    override val recordedAt: LocalDateTime,
) : Bitemporal {
    init {
        validateTemporalBounds()
    }
}

enum class PersonType {
    NATURAL,
    JURIDICAL,
}

enum class Residency {
    NATIONAL,
    FOREIGNER,
}

enum class TaxRegime {
    SIMPLES_NACIONAL,
    LUCRO_PRESUMIDO,
    LUCRO_REAL,
    MEI,
}

sealed interface Document {
    val value: String

    @JvmInline
    value class CPF(
        override val value: String,
    ) : Document

    @JvmInline
    value class CNPJ(
        override val value: String,
    ) : Document

    @JvmInline
    value class Foreign(
        override val value: String,
    ) : Document
}
