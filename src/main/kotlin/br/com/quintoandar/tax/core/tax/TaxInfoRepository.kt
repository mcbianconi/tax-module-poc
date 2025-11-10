package br.com.quintoandar.tax.core.tax

import br.com.quintoandar.tax.adapters.csv.TaxInfoCsvLoader
import br.com.quintoandar.tax.core.order.ActorId
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class TaxInfoRepository(
    private val csvLoader: TaxInfoCsvLoader,
) {
    private val storage = mutableListOf<ActorTaxInfo>()

    @PostConstruct
    fun initialize() {
        val taxInfos = csvLoader.load()
        storage.addAll(taxInfos)
    }

    private fun findByActorId(
        actorId: ActorId,
        validAt: LocalDate,
        knownAt: LocalDateTime,
    ): ActorTaxInfo? =
        storage
            .filter { it.actorId == actorId }
            .filter { it.isValidInBitemporalPeriod(validAt, knownAt) }
            .maxByOrNull { it.recordedAt }

    fun findByActorIds(
        actorIds: Set<ActorId>,
        validAt: LocalDate,
        knownAt: LocalDateTime,
    ): Map<ActorId, ActorTaxInfo> =
        actorIds
            .associateWith { actorId ->
                findByActorId(actorId, validAt, knownAt)
            }.filterValues { it != null }
            .mapValues { it.value!! }
}
