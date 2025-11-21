package org.genspectrum.lapis.request

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.instanceOf
import org.junit.jupiter.api.Test

class OrderByFieldExtensionsTest {
    @Test
    fun `GIVEN empty list THEN toOrderBySpec returns ByFields with empty list`() {
        val result = emptyList<OrderByField>().toOrderBySpec()

        assertThat(result, instanceOf(OrderBySpec.ByFields::class.java))
        val byFields = result as OrderBySpec.ByFields
        assertThat(byFields.fields.size, equalTo(0))
    }

    @Test
    fun `GIVEN list with single field THEN toOrderBySpec returns ByFields`() {
        val input = listOf(OrderByField("country", Order.ASCENDING))

        val result = input.toOrderBySpec()

        assertThat(result, instanceOf(OrderBySpec.ByFields::class.java))
        val byFields = result as OrderBySpec.ByFields
        assertThat(byFields.fields.size, equalTo(1))
        assertThat(byFields.fields[0].field, equalTo("country"))
        assertThat(byFields.fields[0].order, equalTo(Order.ASCENDING))
    }

    @Test
    fun `GIVEN list with multiple fields THEN toOrderBySpec returns ByFields`() {
        val input = listOf(
            OrderByField("country", Order.ASCENDING),
            OrderByField("date", Order.DESCENDING),
        )

        val result = input.toOrderBySpec()

        assertThat(result, instanceOf(OrderBySpec.ByFields::class.java))
        val byFields = result as OrderBySpec.ByFields
        assertThat(byFields.fields.size, equalTo(2))
        assertThat(byFields.fields[0].field, equalTo("country"))
        assertThat(byFields.fields[0].order, equalTo(Order.ASCENDING))
        assertThat(byFields.fields[1].field, equalTo("date"))
        assertThat(byFields.fields[1].order, equalTo(Order.DESCENDING))
    }

    @Test
    fun `GIVEN list with 'random' field THEN toOrderBySpec returns Random with null seed`() {
        val input = listOf(OrderByField("random", Order.ASCENDING))

        val result = input.toOrderBySpec()

        assertThat(result, instanceOf(OrderBySpec.Random::class.java))
        val random = result as OrderBySpec.Random
        assertThat(random.seed, equalTo(null))
    }

    @Test
    fun `GIVEN list with 'random(123)' field THEN toOrderBySpec returns Random with seed 123`() {
        val input = listOf(OrderByField("random(123)", Order.ASCENDING))

        val result = input.toOrderBySpec()

        assertThat(result, instanceOf(OrderBySpec.Random::class.java))
        val random = result as OrderBySpec.Random
        assertThat(random.seed, equalTo(123))
    }

    @Test
    fun `GIVEN list with 'random(0)' field THEN toOrderBySpec returns Random with seed 0`() {
        val input = listOf(OrderByField("random(0)", Order.ASCENDING))

        val result = input.toOrderBySpec()

        assertThat(result, instanceOf(OrderBySpec.Random::class.java))
        val random = result as OrderBySpec.Random
        assertThat(random.seed, equalTo(0))
    }

    @Test
    fun `GIVEN list with 'random(999)' field THEN toOrderBySpec returns Random with seed 999`() {
        val input = listOf(OrderByField("random(999)", Order.ASCENDING))

        val result = input.toOrderBySpec()

        assertThat(result, instanceOf(OrderBySpec.Random::class.java))
        val random = result as OrderBySpec.Random
        assertThat(random.seed, equalTo(999))
    }
}
