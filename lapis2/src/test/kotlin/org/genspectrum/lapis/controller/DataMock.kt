package org.genspectrum.lapis.controller

import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode
import io.mockk.every
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.AminoAcidInsertionResponse
import org.genspectrum.lapis.response.AminoAcidMutationResponse
import org.genspectrum.lapis.response.DetailsData
import org.genspectrum.lapis.response.NucleotideInsertionResponse
import org.genspectrum.lapis.response.NucleotideMutationResponse

data class MockData(
    val mockToReturnEmptyData: (SiloQueryModel) -> Unit,
    val mockWithData: (SiloQueryModel) -> Unit,
    val expectedJson: String,
    val expectedCsv: String,
    val expectedTsv: String,
) {
    companion object {
        inline fun <reified Arg, Data> create(
            crossinline siloQueryModelMockCall: (SiloQueryModel) -> (Arg) -> List<Data>,
            modelData: List<Data>,
            expectedJson: String,
            expectedCsv: String,
            expectedTsv: String,
        ) = MockData(
            { modelMock -> every { siloQueryModelMockCall(modelMock)(any()) } returns emptyList() },
            { modelMock -> every { siloQueryModelMockCall(modelMock)(any()) } returns modelData },
            expectedJson,
            expectedCsv,
            expectedTsv,
        )
    }
}

object MockDataForEndpoints {
    fun getMockData(endpoint: String) = when (endpoint) {
        DETAILS_ROUTE -> details
        AGGREGATED_ROUTE -> aggregated
        NUCLEOTIDE_MUTATIONS_ROUTE -> nucleotideMutations
        AMINO_ACID_MUTATIONS_ROUTE -> aminoAcidMutations
        NUCLEOTIDE_INSERTIONS_ROUTE -> nucleotideInsertions
        AMINO_ACID_INSERTIONS_ROUTE -> aminoAcidInsertions
        else -> throw IllegalArgumentException("Test issue: no mock data for endpoint $endpoint")
    }

    private val aggregated = MockData.create(
        siloQueryModelMockCall = { it::getAggregated },
        modelData = listOf(
            AggregationData(
                0,
                mapOf("country" to TextNode("Switzerland"), "age" to IntNode(42)),
            ),
        ),
        expectedJson = "TODO",
        expectedCsv = """
            country,age,count
            Switzerland,42,0
        """.trimIndent(),
        expectedTsv = """
           country	age	count
           Switzerland	42	0
        """.trimIndent(),
    )

    private val details = MockData.create(
        siloQueryModelMockCall = { it::getDetails },
        modelData = listOf(
            DetailsData(
                mapOf(
                    "country" to TextNode("Switzerland"),
                    "age" to IntNode(42),
                    "floatValue" to DoubleNode(3.14),
                ),
            ),
            DetailsData(
                mapOf(
                    "country" to TextNode("Switzerland"),
                    "age" to IntNode(43),
                    "floatValue" to NullNode.instance,
                ),
            ),
        ),
        expectedJson = "TODO",
        expectedCsv = """
            country,age,floatValue
            Switzerland,42,3.14
            Switzerland,43,null
        """.trimIndent(),
        expectedTsv = """
            country	age	floatValue
            Switzerland	42	3.14
            Switzerland	43	null
        """.trimIndent(),
    )

    private val nucleotideMutations = MockData.create(
        siloQueryModelMockCall = { it::computeNucleotideMutationProportions },
        modelData = listOf(
            NucleotideMutationResponse(
                "sequenceName:1234",
                2345,
                0.987,
            ),
        ),
        expectedJson = "TODO",
        expectedCsv = """
            mutation,count,proportion
            sequenceName:1234,2345,0.987
        """.trimIndent(),
        expectedTsv = """
            mutation	count	proportion
            sequenceName:1234	2345	0.987
        """.trimIndent(),
    )

    private val aminoAcidMutations = MockData.create(
        siloQueryModelMockCall = { it::computeAminoAcidMutationProportions },
        modelData = listOf(
            AminoAcidMutationResponse(
                "sequenceName:1234",
                2345,
                0.987,
            ),
        ),
        expectedJson = "TODO",
        expectedCsv = """
            mutation,count,proportion
            sequenceName:1234,2345,0.987
        """.trimIndent(),
        expectedTsv = """
            mutation	count	proportion
            sequenceName:1234	2345	0.987
        """.trimIndent(),
    )

    private val nucleotideInsertions = MockData.create(
        siloQueryModelMockCall = { it::getNucleotideInsertions },
        modelData = listOf(
            NucleotideInsertionResponse(
                "ins_1234:CAGAA",
                41,
            ),
        ),
        expectedJson = "TODO",
        expectedCsv = """
            insertion,count
            ins_1234:CAGAA,41
        """.trimIndent(),
        expectedTsv = """
            insertion	count
            ins_1234:CAGAA	41
        """.trimIndent(),
    )

    private val aminoAcidInsertions = MockData.create(
        siloQueryModelMockCall = { it::getAminoAcidInsertions },
        modelData = listOf(
            AminoAcidInsertionResponse(
                "ins_ORF1a:1234:CAGAA",
                41,
            ),
        ),
        expectedJson = "TODO",
        expectedCsv = """
            insertion,count
            ins_ORF1a:1234:CAGAA,41
        """.trimIndent(),
        expectedTsv = """
            insertion	count
            ins_ORF1a:1234:CAGAA	41
        """.trimIndent(),
    )
}

