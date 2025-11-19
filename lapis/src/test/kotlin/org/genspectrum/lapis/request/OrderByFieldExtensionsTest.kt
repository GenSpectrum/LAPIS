package org.genspectrum.lapis.request

import org.genspectrum.lapis.controller.BadRequestException
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.instanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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

    @Test
    fun `GIVEN list with 'random' and other fields THEN throws BadRequestException`() {
        val input = listOf(
            OrderByField("country", Order.ASCENDING),
            OrderByField("random", Order.ASCENDING),
        )

        val exception = assertThrows<BadRequestException> {
            input.toOrderBySpec()
        }

        assertThat(
            exception.message,
            equalTo(
                "Cannot mix 'random' with other orderBy fields. " +
                    "Use either 'orderBy=random' or 'orderBy=field1,field2'",
            ),
        )
    }

    @Test
    fun `GIVEN list with 'random(123)' and other fields THEN throws BadRequestException`() {
        val input = listOf(
            OrderByField("date", Order.DESCENDING),
            OrderByField("random(123)", Order.ASCENDING),
        )

        val exception = assertThrows<BadRequestException> {
            input.toOrderBySpec()
        }

        assertThat(
            exception.message,
            equalTo(
                "Cannot mix 'random' with other orderBy fields. " +
                    "Use either 'orderBy=random' or 'orderBy=field1,field2'",
            ),
        )
    }

    @Test
    fun `GIVEN list with other fields and 'random' THEN throws BadRequestException`() {
        val input = listOf(
            OrderByField("random", Order.ASCENDING),
            OrderByField("country", Order.ASCENDING),
        )

        val exception = assertThrows<BadRequestException> {
            input.toOrderBySpec()
        }

        assertThat(
            exception.message,
            equalTo(
                "Cannot mix 'random' with other orderBy fields. " +
                    "Use either 'orderBy=random' or 'orderBy=field1,field2'",
            ),
        )
    }

    @Test
    fun `GIVEN list with three fields including 'random' THEN throws BadRequestException`() {
        val input = listOf(
            OrderByField("country", Order.ASCENDING),
            OrderByField("random", Order.ASCENDING),
            OrderByField("date", Order.DESCENDING),
        )

        val exception = assertThrows<BadRequestException> {
            input.toOrderBySpec()
        }

        assertThat(
            exception.message,
            equalTo(
                "Cannot mix 'random' with other orderBy fields. " +
                    "Use either 'orderBy=random' or 'orderBy=field1,field2'",
            ),
        )
    }
}
