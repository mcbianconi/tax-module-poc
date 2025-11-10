package br.com.quintoandar.tax.adapters.csv

import br.com.quintoandar.tax.core.tax.ActorTaxInfo
import br.com.quintoandar.tax.core.tax.Document
import br.com.quintoandar.tax.core.tax.PersonType
import br.com.quintoandar.tax.core.tax.Residency
import br.com.quintoandar.tax.core.tax.TaxRegime
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class TaxInfoCsvLoader(
    @param:Value("\${tax.info.csv.path:test-data/tax-info.csv}")
    private val csvPath: String,
) {
    fun load(): List<ActorTaxInfo> {
        val csvContent =
            this::class.java.classLoader
                .getResourceAsStream(csvPath)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: throw IllegalStateException("Arquivo CSV não encontrado: $csvPath")

        return parse(csvContent)
    }

    private fun parse(csvContent: String): List<ActorTaxInfo> =
        csvContent
            .lines()
            .drop(1) // skip header
            .filter { it.isNotBlank() }
            .map { line -> parseLine(line) }

    private fun parseLine(line: String): ActorTaxInfo {
        val row = CsvRow.fromLine(line)

        return ActorTaxInfo(
            actorId = row.actorId,
            document = parseDocument(row.documentType, row.documentValue),
            personType = PersonType.valueOf(row.personType),
            residency = Residency.valueOf(row.residency),
            taxRegime = TaxRegime.valueOf(row.taxRegime),
            validFrom = LocalDate.parse(row.validFrom),
            validUntil = if (row.validTo.isEmpty()) null else LocalDate.parse(row.validTo),
            recordedAt = LocalDateTime.parse(row.recordedAt),
        )
    }

    private fun parseDocument(
        type: String,
        value: String,
    ): Document =
        when (type) {
            "CPF" -> Document.CPF(value)
            "CNPJ" -> Document.CNPJ(value)
            "Foreigner" -> Document.Foreign(value)
            else -> throw IllegalArgumentException("Tipo de documento desconhecido: $type")
        }
}

private data class CsvRow(
    val actorId: String,
    val documentType: String,
    val documentValue: String,
    val personType: String,
    val residency: String,
    val taxRegime: String,
    val validFrom: String,
    val validTo: String,
    val recordedAt: String,
) {
    companion object {
        fun fromLine(line: String): CsvRow {
            val parts = line.split(",").map { it.trim() }
            require(parts.size == 9) { "CSV line deve ter 9 campos: $line" }
            return CsvRow(
                actorId = parts[0],
                documentType = parts[1],
                documentValue = parts[2],
                personType = parts[3],
                residency = parts[4],
                taxRegime = parts[5],
                validFrom = parts[6],
                validTo = parts[7],
                recordedAt = parts[8],
            )
        }
    }
}
