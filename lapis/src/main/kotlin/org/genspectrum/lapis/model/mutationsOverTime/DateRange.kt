package org.genspectrum.lapis.model.mutationsOverTime

import java.time.LocalDate

data class DateRange(
    var dateFrom: LocalDate?,
    var dateTo: LocalDate?,
) {
    fun containsDate(date: LocalDate): Boolean {
        if (dateFrom == null && dateTo == null) {
            return false
        }

        return (dateFrom == null || date >= dateFrom) &&
            (dateTo == null || date <= dateTo)
    }
}
