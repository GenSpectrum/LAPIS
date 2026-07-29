package org.genspectrum.lapis.config

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class ViewRoutingFilterTest {
    @Test
    fun `capability is determined from the endpoint and not a path variable`() {
        assertThat(
            requiredViewCapability(listOf("sample", "alignedAminoAcidSequences", "details")),
            equalTo(ViewCapability.SEQUENCES),
        )
    }

    @Test
    fun `component endpoints require the components capability`() {
        assertThat(
            requiredViewCapability(listOf("component", "queriesOverTime")),
            equalTo(ViewCapability.COMPONENTS),
        )
    }
}
