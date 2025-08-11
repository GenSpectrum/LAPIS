package org.genspectrum.lapis.controller

import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_CSV_VALUE
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_NEWICK_VALUE
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_TSV_VALUE
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_X_FASTA_VALUE
import org.genspectrum.lapis.model.FastaHeaderTemplate
import org.genspectrum.lapis.model.SequencesResponse
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.model.TemplateField
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.DetailsData
import org.genspectrum.lapis.response.ExplicitlyNullable
import org.genspectrum.lapis.response.InsertionResponse
import org.genspectrum.lapis.response.MostCommonAncestorData
import org.genspectrum.lapis.response.MutationResponse
import org.genspectrum.lapis.response.PhyloSubtreeData
import org.genspectrum.lapis.response.SequenceData
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.`is`
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.MediaType.APPLICATION_NDJSON_VALUE
import java.util.stream.Stream

data class MockDataCollection(
    val mockToReturnEmptyData: (SiloQueryModel) -> Unit,
    val mockWithData: (SiloQueryModel) -> Unit,
    val expectedJson: String,
    val expectedCsv: String,
    val expectedTsv: String,
    val fields: List<String>?,
    val phyloTreeField: String? = null,
) {
    enum class DataFormat(
        val fileFormat: String,
        val acceptHeader: String,
    ) {
        PLAIN_JSON("json", APPLICATION_JSON_VALUE),
        NESTED_JSON("json", APPLICATION_JSON_VALUE),
        CSV("csv", TEXT_CSV_VALUE),
        CSV_WITHOUT_HEADERS("csv", TEXT_CSV_VALUE),
        TSV("tsv", TEXT_TSV_VALUE),
    }

    companion object {
        inline fun <reified Arg, Data> create(
            crossinline siloQueryModelMockCall: (SiloQueryModel) -> (Arg) -> Stream<Data>,
            modelData: List<Data>,
            expectedJson: String,
            expectedCsv: String,
            expectedTsv: String,
            fields: List<String>? = null,
            phyloTreeField: String? = null,
        ) = MockDataCollection(
            { modelMock -> every { siloQueryModelMockCall(modelMock)(any()) } returns Stream.empty() },
            { modelMock -> every { siloQueryModelMockCall(modelMock)(any()) } returns modelData.stream() },
            expectedJson,
            expectedCsv,
            expectedTsv,
            fields,
            phyloTreeField,
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

                DataFormat.CSV_WITHOUT_HEADERS -> {
                    { assertThat(it, `is`(expectedCsv.lines().drop(1).joinToString("\n"))) }
                }

                DataFormat.TSV -> {
                    { assertThat(it, `is`(expectedTsv)) }
                }
            },
            fields = fields,
            phyloTreeField = phyloTreeField,
        )
}

data class SequenceEndpointMockDataCollection(
    val getSequencesResponse: () -> SequencesResponse,
    val mockToReturnEmptyData: (SiloQueryModel) -> Unit,
    val mockWithData: (SiloQueryModel) -> Unit,
    val expectedFasta: String,
    val expectedJson: String,
    val expectedNdjson: String,
) {
    enum class DataFormat(
        val fileFormat: String,
        val acceptHeader: String,
    ) {
        JSON("json", APPLICATION_JSON_VALUE),
        NDJSON("ndjson", APPLICATION_NDJSON_VALUE),
        FASTA("fasta", TEXT_X_FASTA_VALUE),
    }

    companion object {
        fun create(
            getSequencesResponse: () -> SequencesResponse,
            expectedFasta: String,
            expectedJson: String,
            expectedNdjson: String,
        ) = SequenceEndpointMockDataCollection(
            getSequencesResponse = getSequencesResponse,
            mockToReturnEmptyData = { modelMock ->
                every {
                    modelMock.getGenomicSequence(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                    )
                } returns getSequencesResponse()
            },
            mockWithData = { modelMock ->
                every {
                    modelMock.getGenomicSequence(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                    )
                } returns getSequencesResponse()
            },
            expectedFasta = expectedFasta,
            expectedJson = expectedJson,
            expectedNdjson = expectedNdjson,
        )
    }

    fun expecting(dataFormat: DataFormat) =
        MockData(
            mockToReturnEmptyData = mockToReturnEmptyData,
            mockWithData = mockWithData,
            assertDataMatches = when (dataFormat) {
                DataFormat.JSON -> {
                    {
                        val objectMapper = jacksonObjectMapper()
                        val actual = objectMapper.readTree(it)
                        val expectedData = objectMapper.readTree(expectedJson)
                        assertThat(actual, `is`(expectedData))
                    }
                }

                DataFormat.NDJSON -> {
                    { actual ->
                        val objectMapper = jacksonObjectMapper()
                        val actualLines = actual.lines().filter { it.isNotBlank() }
                        val expectedLines = expectedNdjson.lines().filter { it.isNotBlank() }

                        assertThat(actualLines, hasSize(expectedLines.size))

                        actualLines.zip(expectedLines).forEachIndexed { index, (actualLine, expectedLine) ->
                            assertThat(
                                "Line ${index + 1} of $actual was not as expected:",
                                objectMapper.readTree(actualLine),
                                `is`(objectMapper.readTree(expectedLine)),
                            )
                        }
                    }
                }

                DataFormat.FASTA -> {
                    { assertThat(it, `is`(expectedFasta)) }
                }
            },
        )
}

data class TreeEndpointMockDataCollection(
    val mockToReturnEmptyData: (SiloQueryModel) -> Unit,
    val mockWithData: (SiloQueryModel) -> Unit,
    val expectedNewick: String,
    val fields: List<String>?,
    val phyloTreeField: String? = null,
) {
    enum class DataFormat(
        val fileFormat: String,
        val acceptHeader: String,
    ) {
        NEWICK("NEWICK", TEXT_NEWICK_VALUE),
    }

    companion object {
        inline fun <reified Arg, PhyloSubtreeData> create(
            crossinline siloQueryModelMockCall: (SiloQueryModel) -> (Arg) -> Stream<PhyloSubtreeData>,
            modelData: List<PhyloSubtreeData>,
            expectedNewick: String,
            fields: List<String>? = null,
            phyloTreeField: String? = null,
        ) = TreeEndpointMockDataCollection(
            { modelMock -> every { siloQueryModelMockCall(modelMock)(any()) } returns Stream.empty() },
            { modelMock -> every { siloQueryModelMockCall(modelMock)(any()) } returns modelData.stream() },
            expectedNewick,
            fields,
            phyloTreeField,
        )
    }

    fun expecting(dataFormat: DataFormat) =
        MockData(
            mockToReturnEmptyData = mockToReturnEmptyData,
            mockWithData = mockWithData,
            assertDataMatches = when (dataFormat) {
                DataFormat.NEWICK -> {
                    { assertThat(it, `is`(expectedNewick)) }
                }
            },
            fields = fields,
            phyloTreeField = phyloTreeField,
        )
}

data class MockData(
    val mockToReturnEmptyData: (SiloQueryModel) -> Unit,
    val mockWithData: (SiloQueryModel) -> Unit,
    val assertDataMatches: (String) -> Unit,
    val fields: List<String>? = null,
    val phyloTreeField: String? = null,
)

object MockDataForEndpoints {
    fun getMockData(endpoint: String) =
        when (endpoint) {
            DETAILS_ROUTE -> details
            AGGREGATED_ROUTE -> aggregated
            NUCLEOTIDE_MUTATIONS_ROUTE -> nucleotideMutations
            AMINO_ACID_MUTATIONS_ROUTE -> aminoAcidMutations
            NUCLEOTIDE_INSERTIONS_ROUTE -> nucleotideInsertions
            AMINO_ACID_INSERTIONS_ROUTE -> aminoAcidInsertions
            MOST_RECENT_COMMON_ANCESTOR_ROUTE -> mostRecentCommonAncestor
            else -> throw IllegalArgumentException("Test issue: no mock data for endpoint $endpoint")
        }

    fun treeEndpointMockData() =
        TreeEndpointMockDataCollection.create(
            siloQueryModelMockCall = { it::getNewick },
            modelData = listOf(
                PhyloSubtreeData(
                    subtreeNewick = "((node1,node2),node3);",
                    missingNodeCount = 0,
                    missingFromTree = null,
                ),
            ),
            expectedNewick = "((node1,node2),node3);",
            fields = listOf("primaryKey"),
            phyloTreeField = "primaryKey",
        )

    fun sequenceEndpointMockData(sequenceName: String = "main") =
        SequenceEndpointMockDataCollection.create(
            getSequencesResponse = {
                SequencesResponse(
                    sequenceData = listOf<SequenceData>(
                        SequenceData(mapOf("primaryKey" to TextNode("sequence1"), sequenceName to TextNode("CAGAA"))),
                        SequenceData(mapOf("primaryKey" to TextNode("sequence2"), sequenceName to TextNode("CAGAT"))),
                        SequenceData(mapOf("primaryKey" to TextNode("sequence3"), sequenceName to NullNode.instance)),
                    ).stream(),
                    requestedSequenceNames = listOf(sequenceName),
                    fastaHeaderTemplate = FastaHeaderTemplate(
                        templateString = "{primaryKey}",
                        fields = setOf(
                            TemplateField.MetadataField(
                                fieldNameInTemplate = "primaryKey",
                                fieldNameInConfig = "primaryKey",
                            ),
                        ),
                    ),
                )
            },
            expectedFasta = """
                >sequence1
                CAGAA
                >sequence2
                CAGAT
            
            """.trimIndent(),
            expectedJson = """
                [
                    { "primaryKey": "sequence1", "$sequenceName": "CAGAA" },
                    { "primaryKey": "sequence2", "$sequenceName": "CAGAT" },
                    { "primaryKey": "sequence3", "$sequenceName": null }
                ]
            """.trimIndent(),
            expectedNdjson = """
                { "primaryKey": "sequence1", "$sequenceName": "CAGAA" }
                { "primaryKey": "sequence2", "$sequenceName": "CAGAT" }
                { "primaryKey": "sequence3", "$sequenceName": null }
            """.trimIndent(),
        )

    fun sequenceEndpointMockDataForAllSequences() =
        SequenceEndpointMockDataCollection.create(
            getSequencesResponse = {
                SequencesResponse(
                    sequenceData = listOf(
                        SequenceData(
                            mapOf(
                                "primaryKey" to TextNode("key1"),
                                "sequence1" to TextNode("CAGAA"),
                                "sequence2" to TextNode("CAGAT"),
                            ),
                        ),
                        SequenceData(
                            mapOf(
                                "primaryKey" to TextNode("key2"),
                                "sequence1" to TextNode("CAGAT"),
                                "sequence2" to NullNode.instance,
                            ),
                        ),
                        SequenceData(
                            mapOf(
                                "primaryKey" to TextNode("key3"),
                                "sequence1" to NullNode.instance,
                                "sequence2" to TextNode("CAGAC"),
                            ),
                        ),
                        SequenceData(
                            mapOf(
                                "primaryKey" to TextNode("key4"),
                                "sequence1" to NullNode.instance,
                                "sequence2" to NullNode.instance,
                            ),
                        ),
                    ).stream(),
                    requestedSequenceNames = listOf("sequence1", "sequence2"),
                    fastaHeaderTemplate = FastaHeaderTemplate(
                        templateString = "{primaryKey}|{.segment}",
                        fields = setOf(
                            TemplateField.MetadataField(
                                fieldNameInTemplate = "primaryKey",
                                fieldNameInConfig = "primaryKey",
                            ),
                            TemplateField.SegmentField,
                        ),
                    ),
                )
            },
            expectedFasta = """
                >key1|sequence1
                CAGAA
                >key1|sequence2
                CAGAT
                >key2|sequence1
                CAGAT
                >key3|sequence2
                CAGAC
                
            """.trimIndent(),
            expectedJson = """
                [
                    { "primaryKey": "key1", "sequence1": "CAGAA", "sequence2": "CAGAT" },
                    { "primaryKey": "key2", "sequence1": "CAGAT", "sequence2": null },
                    { "primaryKey": "key3", "sequence1": null, "sequence2": "CAGAC" },
                    { "primaryKey": "key4", "sequence1": null, "sequence2": null }
                ]
            """.trimIndent(),
            expectedNdjson = """
                { "primaryKey": "key1", "sequence1": "CAGAA", "sequence2": "CAGAT" }
                { "primaryKey": "key2", "sequence1": "CAGAT", "sequence2": null }
                { "primaryKey": "key3", "sequence1": null, "sequence2": "CAGAC" }
                { "primaryKey": "key4", "sequence1": null, "sequence2": null }
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
        fields = listOf("country", "age"),
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
        fields = listOf("country", "age", "floatValue"),
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
            MutationResponse(
                mutation = "sequenceName:A1234T",
                count = 2345,
                coverage = 3456,
                proportion = 0.987,
                sequenceName = ExplicitlyNullable("sequenceName"),
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
                    "coverage": 3456,
                    "proportion": 0.987,
                    "sequenceName": "sequenceName",
                    "mutationFrom": "A",
                    "mutationTo": "T",
                    "position": 1234
                }
            ]
        """.trimIndent(),
        expectedCsv = """
            mutation,count,coverage,proportion,sequenceName,mutationFrom,mutationTo,position
            sequenceName:A1234T,2345,3456,0.987,sequenceName,A,T,1234
            
        """.trimIndent(),
        expectedTsv = """
            mutation	count	coverage	proportion	sequenceName	mutationFrom	mutationTo	position
            sequenceName:A1234T	2345	3456	0.987	sequenceName	A	T	1234
            
        """.trimIndent(),
    )

    private val aminoAcidMutations = MockDataCollection.create(
        siloQueryModelMockCall = { it::computeAminoAcidMutationProportions },
        modelData = listOf(
            MutationResponse(
                mutation = "sequenceName:A1234T",
                count = 2345,
                coverage = 3456,
                proportion = 0.987,
                sequenceName = ExplicitlyNullable("sequenceName"),
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
                    "coverage": 3456,
                    "proportion": 0.987,
                    "sequenceName": "sequenceName",
                    "mutationFrom": "A",
                    "mutationTo": "T",
                    "position": 1234
                }
            ]
        """.trimIndent(),
        expectedCsv = """
            mutation,count,coverage,proportion,sequenceName,mutationFrom,mutationTo,position
            sequenceName:A1234T,2345,3456,0.987,sequenceName,A,T,1234
            
        """.trimIndent(),
        expectedTsv = """
            mutation	count	coverage	proportion	sequenceName	mutationFrom	mutationTo	position
            sequenceName:A1234T	2345	3456	0.987	sequenceName	A	T	1234
            
        """.trimIndent(),
    )

    private val nucleotideInsertions = MockDataCollection.create(
        siloQueryModelMockCall = { it::getNucleotideInsertions },
        modelData = listOf(
            InsertionResponse(
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

    private val mostRecentCommonAncestor = MockDataCollection.create(
        siloQueryModelMockCall = { it::getMostRecentCommonAncestor },
        modelData = listOf(
            MostCommonAncestorData(
                mrcaNode = "node1",
                missingNodeCount = 0,
                missingFromTree = null,
            ),
        ),
        phyloTreeField = "primaryKey",
        expectedJson = """
            [
                {
                    "mrcaNode": "node1",
                    "missingNodeCount": 0,
                    "missingFromTree": null
                }
            ]
        """.trimIndent(),
        expectedCsv = """
            mrcaNode,missingNodeCount,missingFromTree
            node1,0,

        """.trimIndent(),
        expectedTsv = """
            mrcaNode	missingNodeCount	missingFromTree
            node1	0	
        
        """.trimIndent(),
    )

    private val aminoAcidInsertions = MockDataCollection.create(
        siloQueryModelMockCall = { it::getAminoAcidInsertions },
        modelData = listOf(
            InsertionResponse(
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
