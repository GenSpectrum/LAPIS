package org.genspectrum.lapis.model.mutationsOverTime

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DateRangeTest {
    @Test
    fun `given a date range without start and end dates, containsDate should return false`() {
        val dateRange = DateRange(null, null)
        assertFalse(dateRange.containsDate(LocalDate.parse("2023-10-01")))
    }

    @Test
    fun `given a date range with from and no to dates, containsDate should return true for dates greater than from `() {
        val dateRange = DateRange(LocalDate.parse("2023-01-01"), null)
        assert(dateRange.containsDate(LocalDate.parse("2023-10-01")))
        assertFalse(dateRange.containsDate(LocalDate.parse("2021-10-01")))
    }

    @Test
    fun `given a date range with to and no from dates, containsDate should return true for dates smaller than to `() {
        val dateRange = DateRange(null, LocalDate.parse("2023-01-01"))
        assertFalse(dateRange.containsDate(LocalDate.parse("2023-10-01")))
        assert(dateRange.containsDate(LocalDate.parse("2021-10-01")))
    }

    @Test
    fun `given a date range with from and to dates, containsDate should return true for dates inside range`() {
        val dateRange = DateRange(LocalDate.parse("2022-01-01"), LocalDate.parse("2023-01-01"))
        assert(dateRange.containsDate(LocalDate.parse("2022-10-01")))
        assert(dateRange.containsDate(LocalDate.parse("2022-01-01")))
        assert(dateRange.containsDate(LocalDate.parse("2023-01-01")))

        assertFalse(dateRange.containsDate(LocalDate.parse("2021-10-01")))
        assertFalse(dateRange.containsDate(LocalDate.parse("2024-10-01")))
    }
}
