package org.genspectrum.lapis.request

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
}
