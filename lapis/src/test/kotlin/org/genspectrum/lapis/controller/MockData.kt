package org.genspectrum.lapis.controller

import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_CSV_VALUE
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_TSV_VALUE
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.AminoAcidInsertionResponse
import org.genspectrum.lapis.response.AminoAcidMutationResponse
import org.genspectrum.lapis.response.DetailsData
import org.genspectrum.lapis.response.NucleotideInsertionResponse
import org.genspectrum.lapis.response.NucleotideMutationResponse
import org.genspectrum.lapis.response.SequenceData
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import java.util.stream.Stream

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
        CSV("csv", TEXT_CSV_VALUE),
        TSV("tsv", TEXT_TSV_VALUE),
    }

    companion object {
        inline fun <reified Arg, Data> create(
            crossinline siloQueryModelMockCall: (SiloQueryModel) -> (Arg) -> Stream<Data>,
            modelData: List<Data>,
            expectedJson: String,
            expectedCsv: String,
            expectedTsv: String,
        ) = MockDataCollection(
            { modelMock -> every { siloQueryModelMockCall(modelMock)(any()) } returns Stream.empty() },
            { modelMock -> every { siloQueryModelMockCall(modelMock)(any()) } returns modelData.stream() },
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
        fun createForFastaEndpoint(
            sequenceData: List<SequenceData>,
            expectedFasta: String,
        ) = MockData(
            { modelMock -> every { modelMock.getGenomicSequence(any(), any(), any()) } returns Stream.empty() },
            { modelMock ->
                every {
                    modelMock.getGenomicSequence(
                        any(),
                        any(),
                        any(),
                    )
                } returns sequenceData.stream()
            },
            expectedFasta,
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
        sequenceData = listOf(
            SequenceData("sequence1", "CAGAA"),
            SequenceData("sequence2", "CAGAA"),
        ),
        expectedFasta = """
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
            Switzerland,43,
            
        """.trimIndent(),
        expectedTsv = """
            country	age	floatValue
            Switzerland	42	3.14
            Switzerland	43	
            
        """.trimIndent(),
    )

    private val nucleotideMutations = MockDataCollection.create(
        siloQueryModelMockCall = { it::computeNucleotideMutationProportions },
        modelData = listOf(
            NucleotideMutationResponse(
                mutation = "sequenceName:A1234T",
                count = 2345,
                proportion = 0.987,
                sequenceName = "sequenceName",
                mutationFrom = "A",
                mutationTo = "T",
                position = 1234,
            ),
        ),
        expectedJson = """
            [
                {
                    "mutation": "sequenceName:A1234T",
                    "count": 2345,
                    "proportion": 0.987,
                    "sequenceName": "sequenceName",
                    "mutationFrom": "A",
                    "mutationTo": "T",
                    "position": 1234
                }
            ]
        """.trimIndent(),
        expectedCsv = """
            mutation,count,proportion,sequenceName,mutationFrom,mutationTo,position
            sequenceName:A1234T,2345,0.987,sequenceName,A,T,1234
            
        """.trimIndent(),
        expectedTsv = """
            mutation	count	proportion	sequenceName	mutationFrom	mutationTo	position
            sequenceName:A1234T	2345	0.987	sequenceName	A	T	1234
            
        """.trimIndent(),
    )

    private val aminoAcidMutations = MockDataCollection.create(
        siloQueryModelMockCall = { it::computeAminoAcidMutationProportions },
        modelData = listOf(
            AminoAcidMutationResponse(
                mutation = "sequenceName:A1234T",
                count = 2345,
                proportion = 0.987,
                sequenceName = "sequenceName",
                mutationFrom = "A",
                mutationTo = "T",
                position = 1234,
            ),
        ),
        expectedJson = """
            [
                {
                    "mutation": "sequenceName:A1234T",
                    "count": 2345,
                    "proportion": 0.987,
                    "sequenceName": "sequenceName",
                    "mutationFrom": "A",
                    "mutationTo": "T",
                    "position": 1234
                }
            ]
        """.trimIndent(),
        expectedCsv = """
            mutation,count,proportion,sequenceName,mutationFrom,mutationTo,position
            sequenceName:A1234T,2345,0.987,sequenceName,A,T,1234
            
        """.trimIndent(),
        expectedTsv = """
            mutation	count	proportion	sequenceName	mutationFrom	mutationTo	position
            sequenceName:A1234T	2345	0.987	sequenceName	A	T	1234
            
        """.trimIndent(),
    )

    private val nucleotideInsertions = MockDataCollection.create(
        siloQueryModelMockCall = { it::getNucleotideInsertions },
        modelData = listOf(
            NucleotideInsertionResponse(
                insertion = "ins_1234:CAGAA",
                count = 41,
                insertedSymbols = "CAGAA",
                position = 1234,
                sequenceName = "sequenceName",
            ),
        ),
        expectedJson = """
            [
                {
                    "insertion": "ins_1234:CAGAA",
                    "count": 41,
                    "insertedSymbols": "CAGAA",
                    "position": 1234,
                    "sequenceName": "sequenceName"
                }
            ]
        """.trimIndent(),
        expectedCsv = """
            insertion,count,insertedSymbols,position,sequenceName
            ins_1234:CAGAA,41,CAGAA,1234,sequenceName
            
        """.trimIndent(),
        expectedTsv = """
            insertion	count	insertedSymbols	position	sequenceName
            ins_1234:CAGAA	41	CAGAA	1234	sequenceName
            
        """.trimIndent(),
    )

    private val aminoAcidInsertions = MockDataCollection.create(
        siloQueryModelMockCall = { it::getAminoAcidInsertions },
        modelData = listOf(
            AminoAcidInsertionResponse(
                insertion = "ins_ORF1a:1234:CAGAA",
                count = 41,
                insertedSymbols = "CAGAA",
                position = 1234,
                sequenceName = "ORF1a",
            ),
        ),
        expectedJson = """
            [
                {
                    "insertion": "ins_ORF1a:1234:CAGAA",
                    "count": 41,
                    "insertedSymbols": "CAGAA",
                    "position": 1234,
                    "sequenceName": "ORF1a"
                }
            ]
        """.trimIndent(),
        expectedCsv = """
            insertion,count,insertedSymbols,position,sequenceName
            ins_ORF1a:1234:CAGAA,41,CAGAA,1234,ORF1a
            
        """.trimIndent(),
        expectedTsv = """
            insertion	count	insertedSymbols	position	sequenceName
            ins_ORF1a:1234:CAGAA	41	CAGAA	1234	ORF1a
            
        """.trimIndent(),
    )
}
