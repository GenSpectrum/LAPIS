package org.genspectrum.lapis.model

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.genspectrum.lapis.response.AggregatedResponse
import org.genspectrum.lapis.response.MutationData
import org.genspectrum.lapis.silo.SiloAction
import org.genspectrum.lapis.silo.SiloClient
import org.genspectrum.lapis.silo.SiloQuery
import org.genspectrum.lapis.silo.True
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SiloQueryModelTest {
    @MockK
    lateinit var siloClientMock: SiloClient

    @MockK
    lateinit var siloFilterExpressionMapperMock: SiloFilterExpressionMapper
    private lateinit var underTest: SiloQueryModel

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        underTest = SiloQueryModel(siloClientMock, siloFilterExpressionMapperMock)
    }

    @Test
    fun `aggregate calls the SILO client with an aggregated action`() {
        every { siloClientMock.sendQuery(any<SiloQuery<AggregatedResponse>>()) } returns AggregatedResponse(0)
        every { siloFilterExpressionMapperMock.map(any<Map<String, String>>()) } returns True

        underTest.aggregate(emptyMap())

        verify {
            siloClientMock.sendQuery(
                SiloQuery(SiloAction.aggregated(), True),
            )
        }
    }

    @Test
    fun `computeMutationProportions calls the SILO client with a mutations action`() {
        every { siloClientMock.sendQuery(any<SiloQuery<List<MutationData>>>()) } returns emptyList()
        every { siloFilterExpressionMapperMock.map(any<Map<String, String>>()) } returns True

        underTest.computeMutationProportions(0.5, emptyMap())

        verify {
            siloClientMock.sendQuery(
                SiloQuery(SiloAction.mutations(0.5), True),
            )
        }
    }
}
