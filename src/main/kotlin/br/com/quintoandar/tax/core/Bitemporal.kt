package br.com.quintoandar.tax.core

import java.time.LocalDate
import java.time.LocalDateTime

interface Bitemporal {
    val validFrom: LocalDate
    val validUntil: LocalDate?
    val recordedAt: LocalDateTime

    fun validateTemporalBounds() {
        if (validUntil != null && validFrom >= validUntil) {
            throw TemporalBoundsException(this)
        }
    }

    fun isValidInBitemporalPeriod(
        validAt: LocalDate,
        knownAt: LocalDateTime,
    ): Boolean {
        val validTimeOk = validFrom <= validAt && (validUntil == null || validAt <= validUntil)
        val recordTimeOk = recordedAt <= knownAt
        return validTimeOk && recordTimeOk
    }
}

class TemporalBoundsException(
    bitemporal: Bitemporal,
) : RuntimeException("temporal bounds not valid for $bitemporal")
