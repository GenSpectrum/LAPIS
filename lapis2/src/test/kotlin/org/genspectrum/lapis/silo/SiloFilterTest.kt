package org.genspectrum.lapis.silo

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class SiloFilterTest {
    @Test
    fun `Query serializes correctly to JSON`() {
        val underTest = SiloQuery(SiloAction.aggregated(), StringEquals("theColumn", "theValue"))

        val result = jacksonObjectMapper().writeValueAsString(underTest)

        val expected = """
            {
                "action": {
                    "type": "Aggregated"
                },
                "filter": {
                    "type": "StringEquals",
                    "column": "theColumn",
                    "value": "theValue"
                }
            }
        """
        assertThat(jacksonObjectMapper().readTree(result), equalTo(jacksonObjectMapper().readTree(expected)))
    }
}
