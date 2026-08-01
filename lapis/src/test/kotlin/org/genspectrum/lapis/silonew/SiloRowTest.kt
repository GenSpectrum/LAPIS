package org.genspectrum.lapis.silonew

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class SiloRowTest {
    @Test
    fun `GIVEN supported values THEN typed getters return exact types`() {
        val date = LocalDate.of(2024, 2, 3)
        val row = SiloRow(
            mapOf(
                "string" to "value",
                "int" to 1,
                "long" to 2L,
                "float" to 3.5f,
                "double" to 4.5,
                "boolean" to true,
                "date" to date,
            ),
        )

        assertThat(row.getString("string"), equalTo("value"))
        assertThat(row.getInt("int"), equalTo(1))
        assertThat(row.getLong("long"), equalTo(2L))
        assertThat(row.getFloat("float"), equalTo(3.5f))
        assertThat(row.getDouble("double"), equalTo(4.5))
        assertThat(row.getBoolean("boolean"), equalTo(true))
        assertThat(row.getDate("date"), equalTo(date))
    }

    @Test
    fun `GIVEN missing or null values THEN typed getters return null`() {
        val row = SiloRow(mapOf("null" to null))

        assertThat(row.getString("missing"), nullValue())
        assertThat(row.getString("null"), nullValue())
        assertThat(row.getLong("missing"), nullValue())
    }

    @Test
    fun `GIVEN a value with another type THEN typed getter describes the mismatch`() {
        val exception = assertThrows<SiloRowTypeException> {
            SiloRow(mapOf("count" to 3L)).getInt("count")
        }

        assertThat(exception.message, containsString("count"))
        assertThat(exception.message, containsString("Int"))
        assertThat(exception.message, containsString("Long"))
    }
}
