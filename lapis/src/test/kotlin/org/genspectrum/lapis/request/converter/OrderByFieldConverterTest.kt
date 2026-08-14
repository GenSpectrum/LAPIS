package org.genspectrum.lapis.request.converter

import org.genspectrum.lapis.request.Order
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class OrderByFieldConverterTest {
    @Autowired
    private lateinit var orderByFieldConverter: OrderByFieldConverter

    @Test
    fun `GIVEN 'random' THEN converts to OrderByField with field 'random'`() {
        val result = orderByFieldConverter.convert("random")

        assertThat(result.field, equalTo("random"))
        assertThat(result.order, equalTo(Order.ASCENDING))
    }

    @Test
    fun `GIVEN 'random(123)' THEN converts to OrderByField with field 'random(123)'`() {
        val result = orderByFieldConverter.convert("random(123)")

        assertThat(result.field, equalTo("random(123)"))
        assertThat(result.order, equalTo(Order.ASCENDING))
    }

    @Test
    fun `GIVEN 'random(0)' THEN converts to OrderByField with field 'random(0)'`() {
        val result = orderByFieldConverter.convert("random(0)")

        assertThat(result.field, equalTo("random(0)"))
        assertThat(result.order, equalTo(Order.ASCENDING))
    }

    @Test
    fun `GIVEN 'random(999999)' THEN converts to OrderByField with field 'random(999999)'`() {
        val result = orderByFieldConverter.convert("random(999999)")

        assertThat(result.field, equalTo("random(999999)"))
        assertThat(result.order, equalTo(Order.ASCENDING))
    }

    @Test
    fun `GIVEN 'country' THEN converts to OrderByField with field 'country'`() {
        val result = orderByFieldConverter.convert("country")

        assertThat(result.field, equalTo("country"))
        assertThat(result.order, equalTo(Order.ASCENDING))
    }

    @Test
    fun `GIVEN a computed field THEN converts to OrderByField with the same computed field`() {
        val result = orderByFieldConverter.convert("date.isoWeek")

        assertThat(result.field, equalTo("date.isoWeek"))
        assertThat(result.order, equalTo(Order.ASCENDING))
    }

    @Test
    fun `GIVEN a differently-cased computed field THEN converts to the same canonical field as 'fields' would`() {
        val result = orderByFieldConverter.convert("DATE.ISOWEEK")

        assertThat(result.field, equalTo("date.isoWeek"))
        assertThat(result.order, equalTo(Order.ASCENDING))
    }

    @Test
    fun `GIVEN 'count' THEN converts to OrderByField with field 'count' unchanged`() {
        val result = orderByFieldConverter.convert("count")

        assertThat(result.field, equalTo("count"))
        assertThat(result.order, equalTo(Order.ASCENDING))
    }
}
