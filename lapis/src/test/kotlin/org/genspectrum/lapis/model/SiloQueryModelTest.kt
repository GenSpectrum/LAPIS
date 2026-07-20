package org.genspectrum.lapis.model

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.genspectrum.lapis.config.DatabaseMetadata
import org.genspectrum.lapis.config.MetadataType
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.config.ReferenceSequenceSchema
import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.controller.mutationData
import org.genspectrum.lapis.controller.mutationProportionsRequest
import org.genspectrum.lapis.controller.sequenceFiltersRequest
import org.genspectrum.lapis.databaseConfig
import org.genspectrum.lapis.request.CaseInsensitiveFieldsCleaner
import org.genspectrum.lapis.request.CommonSequenceFilters
import org.genspectrum.lapis.request.Field
import org.genspectrum.lapis.request.MutationsField
import org.genspectrum.lapis.request.Order
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.request.OrderBySpec
import org.genspectrum.lapis.request.SequenceFiltersRequest
import org.genspectrum.lapis.request.SequenceFiltersRequestWithFields
import org.genspectrum.lapis.request.toOrderBySpec
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.DetailsData
import org.genspectrum.lapis.response.ExplicitlyNullable
import org.genspectrum.lapis.response.InsertionData
import org.genspectrum.lapis.response.InsertionResponse
import org.genspectrum.lapis.response.MutationData
import org.genspectrum.lapis.response.MutationResponse
import org.genspectrum.lapis.response.SequenceData
import org.genspectrum.lapis.silo.SequenceType
import org.genspectrum.lapis.silo.SiloAction
import org.genspectrum.lapis.silo.SiloClient
import org.genspectrum.lapis.silo.SiloQuery
import org.genspectrum.lapis.silo.True
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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

    private val testDatabaseConfig = databaseConfig(
        metadata = listOf(
            DatabaseMetadata(name = "accession", type = MetadataType.STRING),
            DatabaseMetadata(name = "age", type = MetadataType.INT),
            DatabaseMetadata(name = "qc", type = MetadataType.FLOAT),
            DatabaseMetadata(name = "isBoolean", type = MetadataType.BOOLEAN),
            DatabaseMetadata(name = "date", type = MetadataType.DATE),
            DatabaseMetadata(name = "primaryKey", type = MetadataType.STRING),
            DatabaseMetadata(
                name = "pangoLineage",
                type = MetadataType.STRING,
                generateLineageIndex = "pangoLineageDefinition.yaml",
            ),
        ),
        primaryKey = "primaryKey",
    )

    private val fastaHeaderTemplateParser = FastaHeaderTemplateParser(
        caseInsensitiveFieldsCleaner = CaseInsensitiveFieldsCleaner(
            databaseConfig = testDatabaseConfig,
        ),
    )

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        underTest = SiloQueryModel(
            siloClient = siloClientMock,
            siloFilterExpressionMapper = siloFilterExpressionMapperMock,
            referenceGenomeSchema = referenceGenomeSchemaMock,
            fastaHeaderTemplateParser = fastaHeaderTemplateParser,
            databaseConfig = testDatabaseConfig,
        )
    }

    private fun aggregatedRequest(
        fields: List<Field>,
        includeSublineagesFor: String? = null,
    ) = SequenceFiltersRequestWithFields(
        emptyMap(),
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList(),
        fields,
        OrderBySpec.EMPTY,
        includeSublineagesFor = includeSublineagesFor,
    )

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
                OrderBySpec.EMPTY,
            ),
        )

        verify {
            siloClientMock.sendQuery(
                SiloQuery(SiloAction.aggregated(emptyList()), True),
            )
        }
    }

    @Test
    fun `GIVEN includeSublineagesFor names the sole lineage field THEN sends a sublineage-inclusive aggregation`() {
        every { siloClientMock.sendQuery(any<SiloQuery<AggregationData>>()) } returns Stream.empty()
        every { siloFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True

        underTest.getAggregated(
            aggregatedRequest(
                fields = listOf(Field("pangoLineage")),
                includeSublineagesFor = "pangoLineage",
            ),
        )

        verify {
            siloClientMock.sendQuery(
                SiloQuery(
                    SiloAction.aggregated(listOf("pangoLineage"), includeSublineagesFor = "pangoLineage"),
                    True,
                ),
            )
        }
    }

    @Test
    fun `GIVEN includeSublineagesFor with different casing THEN normalizes to the canonical field name`() {
        every { siloClientMock.sendQuery(any<SiloQuery<AggregationData>>()) } returns Stream.empty()
        every { siloFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True

        underTest.getAggregated(
            aggregatedRequest(
                fields = listOf(Field("pangoLineage")),
                includeSublineagesFor = "PANGOLINEAGE",
            ),
        )

        verify {
            siloClientMock.sendQuery(
                SiloQuery(
                    SiloAction.aggregated(listOf("pangoLineage"), includeSublineagesFor = "pangoLineage"),
                    True,
                ),
            )
        }
    }

    @Test
    fun `GIVEN includeSublineagesFor names an unknown field THEN throws BadRequestException`() {
        val exception = assertThrows<BadRequestException> {
            underTest.getAggregated(
                aggregatedRequest(
                    fields = listOf(Field("notAField")),
                    includeSublineagesFor = "notAField",
                ),
            )
        }

        assertThat(exception.message, containsString("Unknown field 'notAField'"))
    }

    @Test
    fun `GIVEN includeSublineagesFor names a non-lineage field THEN throws BadRequestException`() {
        val exception = assertThrows<BadRequestException> {
            underTest.getAggregated(
                aggregatedRequest(
                    fields = listOf(Field("accession")),
                    includeSublineagesFor = "accession",
                ),
            )
        }

        assertThat(exception.message, containsString("not a lineage field"))
    }

    @Test
    fun `GIVEN includeSublineagesFor field is not present in fields THEN throws BadRequestException`() {
        val exception = assertThrows<BadRequestException> {
            underTest.getAggregated(
                aggregatedRequest(
                    fields = listOf(Field("accession")),
                    includeSublineagesFor = "pangoLineage",
                ),
            )
        }

        assertThat(exception.message, containsString("must also be present in 'fields'"))
    }

    @Test
    fun `GIVEN includeSublineagesFor combined with a second group-by field THEN throws BadRequestException`() {
        val exception = assertThrows<BadRequestException> {
            underTest.getAggregated(
                aggregatedRequest(
                    fields = listOf(Field("pangoLineage"), Field("accession")),
                    includeSublineagesFor = "pangoLineage",
                ),
            )
        }

        assertThat(exception.message, containsString("must contain exactly the single lineage field"))
    }

    @Test
    fun `GIVEN no fields specified THEN getDetails uses all metadata fields`() {
        every { siloClientMock.sendQuery(any<SiloQuery<DetailsData>>()) } returns Stream.empty()
        every { siloFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True

        underTest.getDetails(
            SequenceFiltersRequestWithFields(
                emptyMap(),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                OrderBySpec.EMPTY,
            ),
        )

        verify {
            siloClientMock.sendQuery(
                SiloQuery(
                    SiloAction.details(
                        listOf("accession", "age", "qc", "isBoolean", "date", "primaryKey", "pangoLineage"),
                    ),
                    True,
                ),
            )
        }
    }

    @Test
    fun `GIVEN fields specified THEN getDetails uses only those fields`() {
        every { siloClientMock.sendQuery(any<SiloQuery<DetailsData>>()) } returns Stream.empty()
        every { siloFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True

        underTest.getDetails(
            SequenceFiltersRequestWithFields(
                emptyMap(),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                listOf(Field("accession"), Field("date")),
                OrderBySpec.EMPTY,
            ),
        )

        verify {
            siloClientMock.sendQuery(
                SiloQuery(
                    SiloAction.details(listOf("accession", "date")),
                    True,
                ),
            )
        }
    }

    @Test
    fun `computeNucleotideMutationProportions calls the SILO client with a mutations action`() {
        every { siloClientMock.sendQuery(any<SiloQuery<MutationData>>()) } returns Stream.empty()
        every { siloFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True
        every { referenceGenomeSchemaMock.isSingleSegmented() } returns true

        underTest.computeNucleotideMutationProportions(mutationProportionsRequest(minProportion = 0.5))

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

        val result = underTest.computeNucleotideMutationProportions(mutationProportionsRequest()).toList()

        val expectedMutation =
            MutationResponse(
                mutation = "A1234B",
                count = 1234,
                coverage = 2345,
                proportion = 0.1234,
                sequenceName = ExplicitlyNullable(null),
                mutationFrom = "A",
                mutationTo = "B",
                position = 1234,
            )
        assertThat(result, equalTo(listOf(expectedMutation)))
    }

    @Test
    fun `GIVEN singleSegmented and fields = position WHEN get nuc mutations THEN sequence name is null`() {
        every { siloClientMock.sendQuery(any<SiloQuery<MutationData>>()) } returns
            Stream.of(mutationData(position = 123))
        every { siloFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True
        every { referenceGenomeSchemaMock.isSingleSegmented() } returns true

        val result =
            underTest.computeNucleotideMutationProportions(
                mutationProportionsRequest(fields = listOf(MutationsField.POSITION)),
            )
                .toList()

        val expectedMutation =
            MutationResponse(
                mutation = null,
                count = null,
                coverage = null,
                proportion = null,
                sequenceName = null,
                mutationFrom = null,
                mutationTo = null,
                position = 123,
            )
        assertThat(result, equalTo(listOf(expectedMutation)))
    }

    @Test
    fun `GIVEN singleSegmented and fields=sequenceName WHEN get nuc mutations THEN has explicit null sequence name`() {
        every { siloClientMock.sendQuery(any<SiloQuery<MutationData>>()) } returns Stream.of(
            mutationData(
                sequenceName = "sequenceName",
            ),
        )
        every { siloFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True
        every { referenceGenomeSchemaMock.isSingleSegmented() } returns true

        val result = underTest.computeNucleotideMutationProportions(
            mutationProportionsRequest(fields = listOf(MutationsField.SEQUENCE_NAME)),
        ).toList()

        val expectedMutation = MutationResponse(
            mutation = null,
            count = null,
            coverage = null,
            proportion = null,
            sequenceName = ExplicitlyNullable(null),
            mutationFrom = null,
            mutationTo = null,
            position = null,
        )
        assertThat(result, equalTo(listOf(expectedMutation)))
    }

    @Test
    fun `GIVEN multiSegmented and fields = mutation WHEN get nuc mutations THEN sequence name is null`() {
        every {
            siloClientMock.sendQuery(
                match<SiloQuery<MutationData>> {
                    when (val action = it.action) {
                        is SiloAction.MutationsAction -> action.fields.contains(MutationsField.SEQUENCE_NAME.value)
                        else -> false
                    }
                },
            )
        } returns Stream.of(
            mutationData(
                mutation = "A1234B",
                sequenceName = "sequenceName",
            ),
        )
        every { siloFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True
        every { referenceGenomeSchemaMock.isSingleSegmented() } returns false

        val result = underTest.computeNucleotideMutationProportions(
            mutationProportionsRequest(fields = listOf(MutationsField.MUTATION)),
        ).toList()

        val expectedMutation = MutationResponse(
            mutation = "sequenceName:A1234B",
            count = null,
            coverage = null,
            proportion = null,
            sequenceName = null,
            mutationFrom = null,
            mutationTo = null,
            position = null,
        )
        assertThat(result, equalTo(listOf(expectedMutation)))
    }

    @Test
    fun `computeNucleotideMutationProportions includes segmentName if singleSegmentedSequenceFeature is not enabled`() {
        every { siloClientMock.sendQuery(any<SiloQuery<MutationData>>()) } returns Stream.of(someMutationData)
        every { siloFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True
        every { referenceGenomeSchemaMock.isSingleSegmented() } returns false

        val result = underTest.computeNucleotideMutationProportions(mutationProportionsRequest()).toList()

        val expectedMutation = MutationResponse(
            mutation = "sequenceName:A1234B",
            count = 1234,
            coverage = 2345,
            proportion = 0.1234,
            sequenceName = ExplicitlyNullable("sequenceName"),
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

        val result = underTest.computeAminoAcidMutationProportions(mutationProportionsRequest()).toList()

        val expectedMutation = MutationResponse(
            mutation = "sequenceName:A1234B",
            count = 1234,
            coverage = 2345,
            proportion = 0.1234,
            sequenceName = ExplicitlyNullable("sequenceName"),
            mutationFrom = "A",
            mutationTo = "B",
            position = 1234,
        )
        assertThat(result, equalTo(listOf(expectedMutation)))
    }

    @Test
    fun `GIVEN fields = mutation WHEN getting amino acid mutations THEN sequence name is null`() {
        every {
            siloClientMock.sendQuery(
                match<SiloQuery<MutationData>> {
                    when (val action = it.action) {
                        is SiloAction.AminoAcidMutationsAction -> action.fields.contains(
                            MutationsField.SEQUENCE_NAME.value,
                        )

                        else -> false
                    }
                },
            )
        } returns Stream.of(
            mutationData(
                mutation = "A1234B",
                sequenceName = "sequenceName",
            ),
        )
        every { siloFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True

        val result = underTest.computeAminoAcidMutationProportions(
            mutationProportionsRequest(fields = listOf(MutationsField.MUTATION)),
        ).toList()

        val expectedMutation = MutationResponse(
            mutation = "sequenceName:A1234B",
            count = null,
            coverage = null,
            proportion = null,
            sequenceName = null,
            mutationFrom = null,
            mutationTo = null,
            position = null,
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
                OrderBySpec.EMPTY,
            ),
        ).toList()

        val expectedInsertion = InsertionResponse(
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
                OrderBySpec.EMPTY,
            ),
        ).toList()

        val expectedInsertion = InsertionResponse(
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
        every { referenceGenomeSchemaMock.getSequenceNameFromCaseInsensitiveName("someSequenceName") } returns
            "someSequenceName"

        underTest.getGenomicSequence(
            sequenceFilters = SequenceFiltersRequest(
                emptyMap(),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                OrderBySpec.EMPTY,
            ),
            sequenceType = SequenceType.ALIGNED,
            sequenceNames = listOf("someSequenceName"),
            rawFastaHeaderTemplate = "{primaryKey}{date}{.segment}",
            sequenceSymbolType = SequenceSymbolType.NUCLEOTIDE,
        )

        verify {
            siloClientMock.sendQuery(
                SiloQuery(
                    SiloAction.genomicSequence(
                        type = SequenceType.ALIGNED,
                        sequenceNames = listOf("someSequenceName"),
                        additionalFields = listOf("primaryKey", "date"),
                    ),
                    True,
                ),
            )
        }
    }

    @Test
    fun `GIVEN request with unaligned sequences WHEN getting genomic sequences THEN maps sequence names`() {
        every { siloClientMock.sendQuery(any<SiloQuery<SequenceData>>()) } returns Stream.empty()
        every { siloFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True

        referenceGenomeSchemaMock = ReferenceGenomeSchema(
            nucleotideSequences = listOf(ReferenceSequenceSchema("Segment1"), ReferenceSequenceSchema("Segment2")),
            genes = emptyList(),
        )
        underTest = SiloQueryModel(
            siloClient = siloClientMock,
            siloFilterExpressionMapper = siloFilterExpressionMapperMock,
            referenceGenomeSchema = referenceGenomeSchemaMock,
            fastaHeaderTemplateParser = fastaHeaderTemplateParser,
            databaseConfig = testDatabaseConfig,
        )

        underTest.getGenomicSequence(
            sequenceFilters = sequenceFiltersRequest(
                sequenceFilters = emptyMap(),
                orderByFields = listOf(
                    OrderByField(field = "primaryKey", order = Order.ASCENDING),
                    OrderByField(field = "segment1", order = Order.DESCENDING),
                ),
            ),
            sequenceType = SequenceType.UNALIGNED,
            sequenceNames = listOf("segment1", "segment2"),
            rawFastaHeaderTemplate = "{primaryKey}{date}{.gene}",
            sequenceSymbolType = SequenceSymbolType.AMINO_ACID,
        )

        verify {
            siloClientMock.sendQuery(
                SiloQuery(
                    SiloAction.genomicSequence(
                        type = SequenceType.UNALIGNED,
                        sequenceNames = listOf("unaligned_Segment1", "unaligned_Segment2"),
                        additionalFields = listOf("primaryKey", "date"),
                        orderByFields = listOf(
                            OrderByField(field = "primaryKey", order = Order.ASCENDING),
                            OrderByField(field = "unaligned_Segment1", order = Order.DESCENDING),
                        ).toOrderBySpec(),
                    ),
                    True,
                ),
            )
        }
    }
}
