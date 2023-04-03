package org.genspectrum.lapis.model

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.genspectrum.lapis.response.AggregatedResponse
import org.genspectrum.lapis.silo.SiloAction
import org.genspectrum.lapis.silo.SiloClient
import org.genspectrum.lapis.silo.SiloQuery
import org.genspectrum.lapis.silo.True
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AggregatedModelTest {
    @MockK
    lateinit var siloClientMock: SiloClient

    @MockK
    lateinit var siloFilterExpressionMapperMock: SiloFilterExpressionMapper
    private lateinit var underTest: AggregatedModel

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        underTest = AggregatedModel(siloClientMock, siloFilterExpressionMapperMock)
    }

    @Test
    fun `given empty filter parameters then handleRequest should call the SiloClient with MatchAll SiloQuery`() {
        val filterParameter = emptyMap<String, String>()
        every { siloClientMock.sendQuery(any<SiloQuery<AggregatedResponse>>()) } returns AggregatedResponse(0)
        every { siloFilterExpressionMapperMock.map(any<Map<String, String>>()) } returns True

        underTest.handleRequest(filterParameter)

        verify {
            siloClientMock.sendQuery(
                SiloQuery(SiloAction.aggregated(), True),
            )
        }
    }
}
