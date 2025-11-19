package org.genspectrum.lapis.request

import com.fasterxml.jackson.databind.ObjectMapper
import org.genspectrum.lapis.controller.BadRequestException
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.instanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class OrderBySpecDeserializerTest {
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `GIVEN array with single field THEN deserializes to ByFields`() {
        val json = """[{"field": "country"}]"""

        val result = objectMapper.readValue(json, OrderBySpec::class.java)

        assertThat(result, instanceOf(OrderBySpec.ByFields::class.java))
        val byFields = result as OrderBySpec.ByFields
        assertThat(byFields.fields.size, equalTo(1))
        assertThat(byFields.fields[0].field, equalTo("country"))
        assertThat(byFields.fields[0].order, equalTo(Order.ASCENDING))
    }

    @Test
    fun `GIVEN array with multiple fields THEN deserializes to ByFields`() {
        val json = """[{"field": "country"}, {"field": "date", "type": "descending"}]"""

        val result = objectMapper.readValue(json, OrderBySpec::class.java)

        assertThat(result, instanceOf(OrderBySpec.ByFields::class.java))
        val byFields = result as OrderBySpec.ByFields
        assertThat(byFields.fields.size, equalTo(2))
        assertThat(byFields.fields[0].field, equalTo("country"))
        assertThat(byFields.fields[0].order, equalTo(Order.ASCENDING))
        assertThat(byFields.fields[1].field, equalTo("date"))
        assertThat(byFields.fields[1].order, equalTo(Order.DESCENDING))
    }

    @Test
    fun `GIVEN array with string field notation THEN deserializes to ByFields`() {
        val json = """["country", "date"]"""

        val result = objectMapper.readValue(json, OrderBySpec::class.java)

        assertThat(result, instanceOf(OrderBySpec.ByFields::class.java))
        val byFields = result as OrderBySpec.ByFields
        assertThat(byFields.fields.size, equalTo(2))
        assertThat(byFields.fields[0].field, equalTo("country"))
        assertThat(byFields.fields[1].field, equalTo("date"))
    }

    @Test
    fun `GIVEN object with random true THEN deserializes to Random with null seed`() {
        val json = """{"random": true}"""

        val result = objectMapper.readValue(json, OrderBySpec::class.java)

        assertThat(result, instanceOf(OrderBySpec.Random::class.java))
        val random = result as OrderBySpec.Random
        assertThat(random.seed, equalTo(null))
    }

    @Test
    fun `GIVEN object with random integer THEN deserializes to Random with seed`() {
        val json = """{"random": 123}"""

        val result = objectMapper.readValue(json, OrderBySpec::class.java)

        assertThat(result, instanceOf(OrderBySpec.Random::class.java))
        val random = result as OrderBySpec.Random
        assertThat(random.seed, equalTo(123))
    }

    @Test
    fun `GIVEN object with random 0 THEN deserializes to Random with seed 0`() {
        val json = """{"random": 0}"""

        val result = objectMapper.readValue(json, OrderBySpec::class.java)

        assertThat(result, instanceOf(OrderBySpec.Random::class.java))
        val random = result as OrderBySpec.Random
        assertThat(random.seed, equalTo(0))
    }

    @Test
    fun `GIVEN object with random false THEN throws BadRequestException`() {
        val json = """{"random": false}"""

        val exception = assertThrows<BadRequestException> {
            objectMapper.readValue(json, OrderBySpec::class.java)
        }

        assertThat(exception.message, equalTo("random must be true or an integer seed"))
    }

    @Test
    fun `GIVEN object with random string THEN throws BadRequestException`() {
        val json = """{"random": "true"}"""

        val exception = assertThrows<BadRequestException> {
            objectMapper.readValue(json, OrderBySpec::class.java)
        }

        assertThat(exception.message, equalTo("random must be true or an integer seed"))
    }

    @Test
    fun `GIVEN plain string THEN throws BadRequestException`() {
        val json = """"country""""

        val exception = assertThrows<BadRequestException> {
            objectMapper.readValue(json, OrderBySpec::class.java)
        }

        assertThat(
            exception.message,
            equalTo("orderBy must be an array of fields or {random: true|<seed>}"),
        )
    }

    @Test
    fun `GIVEN object without random property THEN throws BadRequestException`() {
        val json = """{"field": "country"}"""

        val exception = assertThrows<BadRequestException> {
            objectMapper.readValue(json, OrderBySpec::class.java)
        }

        assertThat(
            exception.message,
            equalTo("orderBy must be an array of fields or {random: true|<seed>}"),
        )
    }

    @Test
    fun `GIVEN array with field 'random' THEN deserializes to Random with null seed`() {
        val json = """[{"field": "random"}]"""

        val result = objectMapper.readValue(json, OrderBySpec::class.java)

        assertThat(result, instanceOf(OrderBySpec.Random::class.java))
        val random = result as OrderBySpec.Random
        assertThat(random.seed, equalTo(null))
    }

    @Test
    fun `GIVEN array with field 'random(123)' THEN deserializes to Random with seed 123`() {
        val json = """[{"field": "random(123)"}]"""

        val result = objectMapper.readValue(json, OrderBySpec::class.java)

        assertThat(result, instanceOf(OrderBySpec.Random::class.java))
        val random = result as OrderBySpec.Random
        assertThat(random.seed, equalTo(123))
    }

    @Test
    fun `GIVEN array with 'random' string notation THEN deserializes to Random with null seed`() {
        val json = """["random"]"""

        val result = objectMapper.readValue(json, OrderBySpec::class.java)

        assertThat(result, instanceOf(OrderBySpec.Random::class.java))
        val random = result as OrderBySpec.Random
        assertThat(random.seed, equalTo(null))
    }

    @Test
    fun `GIVEN array with mixed random and other fields THEN throws BadRequestException`() {
        val json = """[{"field": "country"}, {"field": "random"}]"""

        val exception = assertThrows<BadRequestException> {
            objectMapper.readValue(json, OrderBySpec::class.java)
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
