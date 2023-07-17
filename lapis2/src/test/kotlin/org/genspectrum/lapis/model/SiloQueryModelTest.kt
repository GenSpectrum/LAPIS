package org.genspectrum.lapis.model

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.genspectrum.lapis.request.CommonSequenceFilters
import org.genspectrum.lapis.request.MutationProportionsRequest
import org.genspectrum.lapis.request.SequenceFiltersRequestWithFields
import org.genspectrum.lapis.response.AggregationData
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
        every { siloClientMock.sendQuery(any<SiloQuery<List<AggregationData>>>()) } returns emptyList()
        every { siloFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True

        underTest.aggregate(SequenceFiltersRequestWithFields(emptyMap(), emptyList(), emptyList(), emptyList()))

        verify {
            siloClientMock.sendQuery(
                SiloQuery(SiloAction.aggregated(emptyList()), True),
            )
        }
    }

    @Test
    fun `computeMutationProportions calls the SILO client with a mutations action`() {
        every { siloClientMock.sendQuery(any<SiloQuery<List<MutationData>>>()) } returns emptyList()
        every { siloFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True

        underTest.computeMutationProportions(MutationProportionsRequest(emptyMap(), emptyList(), emptyList(), 0.5))

        verify {
            siloClientMock.sendQuery(
                SiloQuery(SiloAction.mutations(0.5), True),
            )
        }
    }
}
