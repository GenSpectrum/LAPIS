package org.genspectrum.lapis.response

import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class MutationResponseTest(
    @param:Autowired private val objectMapper: ObjectMapper,
) {
    @Test
    fun `GIVEN null sequence name THEN is not in serialized json`() {
        val underTest = MutationResponse(
            mutation = "A123T",
            count = null,
            coverage = null,
            proportion = null,
            sequenceName = null,
            mutationFrom = null,
            mutationTo = null,
            position = null,
        )

        val json = objectMapper.writeValueAsString(underTest)

        assertThat(
            json,
            `is`("""{"mutation":"A123T"}"""),
        )
    }

    @Test
    fun `GIVEN explicitly null sequence name THEN serialized json contains null`() {
        val underTest = MutationResponse(
            mutation = "A123T",
            count = null,
            coverage = null,
            proportion = null,
            sequenceName = ExplicitlyNullable(null),
            mutationFrom = null,
            mutationTo = null,
            position = null,
        )

        val json = objectMapper.writeValueAsString(underTest)

        assertThat(
            json,
            `is`("""{"mutation":"A123T","sequenceName":null}"""),
        )
    }

    @Test
    fun `GIVEN value for sequence name THEN serialized json contains null`() {
        val underTest = MutationResponse(
            mutation = "A123T",
            count = null,
            coverage = null,
            proportion = null,
            sequenceName = ExplicitlyNullable("the sequence"),
            mutationFrom = null,
            mutationTo = null,
            position = null,
        )

        val json = objectMapper.writeValueAsString(underTest)

        assertThat(
            json,
            `is`("""{"mutation":"A123T","sequenceName":"the sequence"}"""),
        )
    }
}
