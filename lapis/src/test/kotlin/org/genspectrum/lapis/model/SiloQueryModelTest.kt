package org.genspectrum.lapis.model

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.request.CommonSequenceFilters
import org.genspectrum.lapis.request.MutationProportionsRequest
import org.genspectrum.lapis.request.SequenceFiltersRequest
import org.genspectrum.lapis.request.SequenceFiltersRequestWithFields
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.AminoAcidInsertionResponse
import org.genspectrum.lapis.response.AminoAcidMutationResponse
import org.genspectrum.lapis.response.InsertionData
import org.genspectrum.lapis.response.MutationData
import org.genspectrum.lapis.response.NucleotideInsertionResponse
import org.genspectrum.lapis.response.NucleotideMutationResponse
import org.genspectrum.lapis.response.SequenceData
import org.genspectrum.lapis.silo.SequenceType
import org.genspectrum.lapis.silo.SiloAction
import org.genspectrum.lapis.silo.SiloClient
import org.genspectrum.lapis.silo.SiloQuery
import org.genspectrum.lapis.silo.True
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.stream.Stream

private val someMutationData = MutationData(
    mutation = "A1234B",
    count = 1234,
    coverage = 2345,
    proportion = 0.1234,
    sequenceName = "sequenceName",
    mutationFrom = "A",
    mutationTo = "B",
    position = 1234,
)

val someInsertionData = InsertionData(
    count = 42,
    insertion = "ins_sequenceName:1234:ABCD",
    insertedSymbols = "ABCD",
    position = 1234,
    sequenceName = "sequenceName",
)

class SiloQueryModelTest {
    @MockK
    lateinit var siloClientMock: SiloClient

    @MockK
    lateinit var referenceGenomeSchemaMock: ReferenceGenomeSchema

    @MockK
    lateinit var siloFilterExpressionMapperMock: SiloFilterExpressionMapper

    private lateinit var underTest: SiloQueryModel

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        underTest = SiloQueryModel(siloClientMock, siloFilterExpressionMapperMock, referenceGenomeSchemaMock)
    }

    @Test
    fun `aggregate calls the SILO client with an aggregated action`() {
        every { siloClientMock.sendQuery(any<SiloQuery<AggregationData>>()) } returns Stream.empty()
        every { siloFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True
        every { referenceGenomeSchemaMock.isSingleSegmented() } returns true

        underTest.getAggregated(
            SequenceFiltersRequestWithFields(
                emptyMap(),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
            ),
        )

        verify {
            siloClientMock.sendQuery(
                SiloQuery(SiloAction.aggregated(emptyList()), True),
            )
        }
    }

    @Test
    fun `computeNucleotideMutationProportions calls the SILO client with a mutations action`() {
        every { siloClientMock.sendQuery(any<SiloQuery<MutationData>>()) } returns Stream.empty()
        every { siloFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True
        every { referenceGenomeSchemaMock.isSingleSegmented() } returns true

        underTest.computeNucleotideMutationProportions(
            MutationProportionsRequest(emptyMap(), emptyList(), emptyList(), emptyList(), emptyList(), 0.5),
        )

        verify {
            siloClientMock.sendQuery(
                SiloQuery(SiloAction.mutations(0.5), True),
            )
        }
    }

    @Test
    fun `computeNucleotideMutationProportions ignores the segmentName if singleSegmentedSequenceFeature is enabled`() {
        every { siloClientMock.sendQuery(any<SiloQuery<MutationData>>()) } returns Stream.of(someMutationData)
        every { siloFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True
        every { referenceGenomeSchemaMock.isSingleSegmented() } returns true

        val result = underTest.computeNucleotideMutationProportions(
            MutationProportionsRequest(emptyMap(), emptyList(), emptyList(), emptyList(), emptyList()),
        ).toList()

        val expectedMutation = NucleotideMutationResponse(
            mutation = "A1234B",
            count = 1234,
            coverage = 2345,
            proportion = 0.1234,
            sequenceName = null,
            mutationFrom = "A",
            mutationTo = "B",
            position = 1234,
        )
        assertThat(result, equalTo(listOf(expectedMutation)))
    }

    @Test
    fun `computeNucleotideMutationProportions includes segmentName if singleSegmentedSequenceFeature is not enabled`() {
        every { siloClientMock.sendQuery(any<SiloQuery<MutationData>>()) } returns Stream.of(someMutationData)
        every { siloFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True
        every { referenceGenomeSchemaMock.isSingleSegmented() } returns false

        val result = underTest.computeNucleotideMutationProportions(
            MutationProportionsRequest(emptyMap(), emptyList(), emptyList(), emptyList(), emptyList()),
        ).toList()

        val expectedMutation = NucleotideMutationResponse(
            mutation = "sequenceName:A1234B",
            count = 1234,
            coverage = 2345,
            proportion = 0.1234,
            sequenceName = "sequenceName",
            mutationFrom = "A",
            mutationTo = "B",
            position = 1234,
        )
        assertThat(result, equalTo(listOf(expectedMutation)))
    }

    @Test
    fun `computeAminoAcidMutationsProportions returns the sequenceName with the position`() {
        every { siloClientMock.sendQuery(any<SiloQuery<MutationData>>()) } returns Stream.of(someMutationData)
        every { siloFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True

        val result = underTest.computeAminoAcidMutationProportions(
            MutationProportionsRequest(emptyMap(), emptyList(), emptyList(), emptyList(), emptyList()),
        ).toList()

        val expectedMutation = AminoAcidMutationResponse(
            mutation = "sequenceName:A1234B",
            count = 1234,
            coverage = 2345,
            proportion = 0.1234,
            sequenceName = "sequenceName",
            mutationFrom = "A",
            mutationTo = "B",
            position = 1234,
        )
        assertThat(result, equalTo(listOf(expectedMutation)))
    }

    @Test
    fun `getNucleotideInsertions ignores the field sequenceName if the nucleotide sequence has one segment`() {
        every { siloClientMock.sendQuery(any<SiloQuery<InsertionData>>()) } returns Stream.of(someInsertionData)
        every { siloFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True
        every { referenceGenomeSchemaMock.isSingleSegmented() } returns true

        val result = underTest.getNucleotideInsertions(
            SequenceFiltersRequest(
                emptyMap(),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
            ),
        ).toList()

        val expectedInsertion = NucleotideInsertionResponse(
            insertion = "ins_sequenceName:1234:ABCD",
            count = 42,
            insertedSymbols = "ABCD",
            position = 1234,
            sequenceName = null,
        )
        assertThat(result, equalTo(listOf(expectedInsertion)))
    }

    @Test
    fun `getAminoAcidInsertions returns the sequenceName with the position`() {
        every { siloClientMock.sendQuery(any<SiloQuery<InsertionData>>()) } returns Stream.of(someInsertionData)
        every { siloFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True

        val result = underTest.getAminoAcidInsertions(
            SequenceFiltersRequest(
                emptyMap(),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
            ),
        ).toList()

        val expectedInsertion = AminoAcidInsertionResponse(
            insertion = "ins_sequenceName:1234:ABCD",
            count = 42,
            insertedSymbols = "ABCD",
            position = 1234,
            sequenceName = "sequenceName",
        )
        assertThat(result, equalTo(listOf(expectedInsertion)))
    }

    @Test
    fun `getGenomicSequence calls the SILO client with a sequence action`() {
        every { siloClientMock.sendQuery(any<SiloQuery<SequenceData>>()) } returns Stream.empty()
        every { siloFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True

        underTest.getGenomicSequence(
            SequenceFiltersRequest(
                emptyMap(),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
            ),
            SequenceType.ALIGNED,
            "someSequenceName",
        )

        verify {
            siloClientMock.sendQuery(
                SiloQuery(SiloAction.genomicSequence(SequenceType.ALIGNED, "someSequenceName"), True),
            )
        }
    }
}
