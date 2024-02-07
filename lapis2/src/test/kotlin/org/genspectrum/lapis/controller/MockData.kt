package org.genspectrum.lapis.controller

import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.AminoAcidInsertionResponse
import org.genspectrum.lapis.response.AminoAcidMutationResponse
import org.genspectrum.lapis.response.DetailsData
import org.genspectrum.lapis.response.NucleotideInsertionResponse
import org.genspectrum.lapis.response.NucleotideMutationResponse
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE

data class MockDataCollection(
    val mockToReturnEmptyData: (SiloQueryModel) -> Unit,
    val mockWithData: (SiloQueryModel) -> Unit,
    val expectedJson: String,
    val expectedCsv: String,
    val expectedTsv: String,
) {
    enum class DataFormat(val fileFormat: String, val acceptHeader: String) {
        PLAIN_JSON("json", APPLICATION_JSON_VALUE),
        NESTED_JSON("json", APPLICATION_JSON_VALUE),
        CSV("csv", TEXT_CSV_HEADER),
        TSV("tsv", TEXT_TSV_HEADER),
    }

    companion object {
        inline fun <reified Arg, Data> create(
            crossinline siloQueryModelMockCall: (SiloQueryModel) -> (Arg) -> List<Data>,
            modelData: List<Data>,
            expectedJson: String,
            expectedCsv: String,
            expectedTsv: String,
        ) = MockDataCollection(
            { modelMock -> every { siloQueryModelMockCall(modelMock)(any()) } returns emptyList() },
            { modelMock -> every { siloQueryModelMockCall(modelMock)(any()) } returns modelData },
            expectedJson,
            expectedCsv,
            expectedTsv,
        )
    }

    fun expecting(dataFormat: DataFormat) =
        MockData(
            mockToReturnEmptyData = mockToReturnEmptyData,
            mockWithData = mockWithData,
            assertDataMatches = when (dataFormat) {
                DataFormat.NESTED_JSON -> {
                    {
                        val objectMapper = jacksonObjectMapper()
                        val actual = objectMapper.readTree(it)["data"]
                        val expectedData = objectMapper.readTree(expectedJson)
                        assertThat("'data' of $it was not as expected:", actual, `is`(expectedData))
                    }
                }

                DataFormat.PLAIN_JSON -> {
                    {
                        val objectMapper = jacksonObjectMapper()
                        val actual = objectMapper.readTree(it)
                        val expectedData = objectMapper.readTree(expectedJson)
                        assertThat(actual, `is`(expectedData))
                    }
                }

                DataFormat.CSV -> {
                    { assertThat(it, `is`(expectedCsv)) }
                }

                DataFormat.TSV -> {
                    { assertThat(it, `is`(expectedTsv)) }
                }
            },
        )
}

data class MockData(
    val mockToReturnEmptyData: (SiloQueryModel) -> Unit,
    val mockWithData: (SiloQueryModel) -> Unit,
    val assertDataMatches: (String) -> Unit,
) {
    constructor(
        mockToReturnEmptyData: (SiloQueryModel) -> Unit,
        mockWithData: (SiloQueryModel) -> Unit,
        expectedStringContent: String,
    ) : this(
        mockToReturnEmptyData,
        mockWithData,
        { assertThat(it, `is`(expectedStringContent)) },
    )

    companion object {
        fun createForFastaEndpoint(fasta: String) =
            MockData(
                { modelMock -> every { modelMock.getGenomicSequence(any(), any(), any()) } returns "" },
                { modelMock -> every { modelMock.getGenomicSequence(any(), any(), any()) } returns fasta },
                fasta,
            )
    }
}

object MockDataForEndpoints {
    fun getMockData(endpoint: String) =
        when (endpoint) {
            DETAILS_ROUTE -> details
            AGGREGATED_ROUTE -> aggregated
            NUCLEOTIDE_MUTATIONS_ROUTE -> nucleotideMutations
            AMINO_ACID_MUTATIONS_ROUTE -> aminoAcidMutations
            NUCLEOTIDE_INSERTIONS_ROUTE -> nucleotideInsertions
            AMINO_ACID_INSERTIONS_ROUTE -> aminoAcidInsertions
            else -> throw IllegalArgumentException("Test issue: no mock data for endpoint $endpoint")
        }

    val fastaMockData = MockData.createForFastaEndpoint(
        """
            >sequence1
            CAGAA
            >sequence2
            CAGAA
        """.trimIndent(),
    )

    private val aggregated = MockDataCollection.create(
        siloQueryModelMockCall = { it::getAggregated },
        modelData = listOf(
            AggregationData(
                0,
                mapOf("country" to TextNode("Switzerland"), "age" to IntNode(42)),
            ),
        ),
        expectedJson = """
            [
                {
                    "country": "Switzerland",
                    "age": 42,
                    "count": 0
                }
            ]
        """.trimIndent(),
        expectedCsv = """
            country,age,count
            Switzerland,42,0
        """.trimIndent(),
        expectedTsv = """
           country	age	count
           Switzerland	42	0
        """.trimIndent(),
    )

    private val details = MockDataCollection.create(
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
        expectedJson = """
            [
                {
                    "country": "Switzerland",
                    "age": 42,
                    "floatValue": 3.14
                },
                {
                    "country": "Switzerland",
                    "age": 43,
                    "floatValue": null
                }
            ]
        """.trimIndent(),
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

    private val nucleotideMutations = MockDataCollection.create(
        siloQueryModelMockCall = { it::computeNucleotideMutationProportions },
        modelData = listOf(
            NucleotideMutationResponse(
                "sequenceName:A1234T",
                2345,
                0.987,
            ),
        ),
        expectedJson = """
            [
                {
                    "mutation": "sequenceName:A1234T",
                    "count": 2345,
                    "proportion": 0.987
                }
            ]
        """.trimIndent(),
        expectedCsv = """
            mutation,count,proportion
            sequenceName:A1234T,2345,0.987
        """.trimIndent(),
        expectedTsv = """
            mutation	count	proportion
            sequenceName:A1234T	2345	0.987
        """.trimIndent(),
    )

    private val aminoAcidMutations = MockDataCollection.create(
        siloQueryModelMockCall = { it::computeAminoAcidMutationProportions },
        modelData = listOf(
            AminoAcidMutationResponse(
                "sequenceName:1234",
                2345,
                0.987,
            ),
        ),
        expectedJson = """
            [
                {
                    "mutation": "sequenceName:1234",
                    "count": 2345,
                    "proportion": 0.987
                }
            ]
        """.trimIndent(),
        expectedCsv = """
            mutation,count,proportion
            sequenceName:1234,2345,0.987
        """.trimIndent(),
        expectedTsv = """
            mutation	count	proportion
            sequenceName:1234	2345	0.987
        """.trimIndent(),
    )

    private val nucleotideInsertions = MockDataCollection.create(
        siloQueryModelMockCall = { it::getNucleotideInsertions },
        modelData = listOf(
            NucleotideInsertionResponse(
                "ins_1234:CAGAA",
                41,
            ),
        ),
        expectedJson = """
            [
                {
                    "insertion": "ins_1234:CAGAA",
                    "count": 41
                }
            ]
        """.trimIndent(),
        expectedCsv = """
            insertion,count
            ins_1234:CAGAA,41
        """.trimIndent(),
        expectedTsv = """
            insertion	count
            ins_1234:CAGAA	41
        """.trimIndent(),
    )

    private val aminoAcidInsertions = MockDataCollection.create(
        siloQueryModelMockCall = { it::getAminoAcidInsertions },
        modelData = listOf(
            AminoAcidInsertionResponse(
                "ins_ORF1a:1234:CAGAA",
                41,
            ),
        ),
        expectedJson = """
            [
                {
                    "insertion": "ins_ORF1a:1234:CAGAA",
                    "count": 41
                }
            ]
        """.trimIndent(),
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
