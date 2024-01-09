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
        every { siloClientMock.sendQuery(any<SiloQuery<List<AggregationData>>>()) } returns emptyList()
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
        every { siloClientMock.sendQuery(any<SiloQuery<List<MutationData>>>()) } returns emptyList()
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
        every { siloClientMock.sendQuery(any<SiloQuery<List<MutationData>>>()) } returns listOf(
            MutationData("A1234B", 1234, 0.1234, "someSequenceName"),
        )
        every { siloFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True
        every { referenceGenomeSchemaMock.isSingleSegmented() } returns true

        val result = underTest.computeNucleotideMutationProportions(
            MutationProportionsRequest(emptyMap(), emptyList(), emptyList(), emptyList(), emptyList()),
        )

        assertThat(result, equalTo(listOf(NucleotideMutationResponse("A1234B", 1234, 0.1234))))
    }

    @Test
    fun `computeNucleotideMutationProportions includes segmentName if singleSegmentedSequenceFeature is not enabled`() {
        every { siloClientMock.sendQuery(any<SiloQuery<List<MutationData>>>()) } returns listOf(
            MutationData("A1234B", 1234, 0.1234, "someSegmentName"),
        )
        every { siloFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True
        every { referenceGenomeSchemaMock.isSingleSegmented() } returns false

        val result = underTest.computeNucleotideMutationProportions(
            MutationProportionsRequest(emptyMap(), emptyList(), emptyList(), emptyList(), emptyList()),
        )

        assertThat(result, equalTo(listOf(NucleotideMutationResponse("someSegmentName:A1234B", 1234, 0.1234))))
    }

    @Test
    fun `computeAminoAcidMutationsProportions returns the sequenceName with the position`() {
        every { siloClientMock.sendQuery(any<SiloQuery<List<MutationData>>>()) } returns listOf(
            MutationData("A1234B", 1234, 0.1234, "someName"),
        )
        every { siloFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True

        val result = underTest.computeAminoAcidMutationProportions(
            MutationProportionsRequest(emptyMap(), emptyList(), emptyList(), emptyList(), emptyList()),
        )

        assertThat(result, equalTo(listOf(AminoAcidMutationResponse("someName:A1234B", 1234, 0.1234))))
    }

    @Test
    fun `getNucleotideInsertions ignores the field sequenceName if the nucleotide sequence has one segment`() {
        every { siloClientMock.sendQuery(any<SiloQuery<List<InsertionData>>>()) } returns listOf(
            InsertionData(42, "ABCD", 1234, "someSequenceName"),
        )
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
        )

        assertThat(result, equalTo(listOf(NucleotideInsertionResponse("ins_1234:ABCD", 42))))
    }

    @Test
    fun `getNucleotideInsertions includes the field sequenceName if the nucleotide sequence has multiple segments`() {
        every { siloClientMock.sendQuery(any<SiloQuery<List<InsertionData>>>()) } returns listOf(
            InsertionData(42, "ABCD", 1234, "someSequenceName"),
        )
        every { siloFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True
        every { referenceGenomeSchemaMock.isSingleSegmented() } returns false

        val result = underTest.getNucleotideInsertions(
            SequenceFiltersRequest(
                emptyMap(),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
            ),
        )

        assertThat(result, equalTo(listOf(NucleotideInsertionResponse("ins_someSequenceName:1234:ABCD", 42))))
    }

    @Test
    fun `getAminoAcidInsertions returns the sequenceName with the position`() {
        every { siloClientMock.sendQuery(any<SiloQuery<List<InsertionData>>>()) } returns listOf(
            InsertionData(42, "ABCD", 1234, "someGene"),
        )
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
        )

        assertThat(result, equalTo(listOf(AminoAcidInsertionResponse("ins_someGene:1234:ABCD", 42))))
    }

    @Test
    fun `getGenomicSequence calls the SILO client with a sequence action`() {
        every { siloClientMock.sendQuery(any<SiloQuery<List<SequenceData>>>()) } returns emptyList()
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
