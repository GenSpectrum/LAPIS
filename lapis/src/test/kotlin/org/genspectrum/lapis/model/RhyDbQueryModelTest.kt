package org.genspectrum.lapis.model

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import org.genspectrum.lapis.config.DatabaseMetadata
import org.genspectrum.lapis.config.MetadataType
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.config.ReferenceSequenceSchema
import org.genspectrum.lapis.controller.mutationData
import org.genspectrum.lapis.controller.mutationProportionsRequest
import org.genspectrum.lapis.controller.sequenceFiltersRequest
import org.genspectrum.lapis.databaseConfig
import org.genspectrum.lapis.request.AggregatedFiltersRequest
import org.genspectrum.lapis.request.CommonSequenceFilters
import org.genspectrum.lapis.request.ComputedField
import org.genspectrum.lapis.request.DetailsFiltersRequest
import org.genspectrum.lapis.request.MutationsField
import org.genspectrum.lapis.request.Order
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.request.OrderBySpec
import org.genspectrum.lapis.request.PlainField
import org.genspectrum.lapis.request.ScalarFunction
import org.genspectrum.lapis.request.SequenceFiltersRequest
import org.genspectrum.lapis.request.converter.CaseInsensitiveFieldsCleaner
import org.genspectrum.lapis.request.toOrderBySpec
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.DetailsData
import org.genspectrum.lapis.response.ExplicitlyNullable
import org.genspectrum.lapis.response.InsertionData
import org.genspectrum.lapis.response.InsertionResponse
import org.genspectrum.lapis.response.MutationData
import org.genspectrum.lapis.response.MutationResponse
import org.genspectrum.lapis.response.SequenceData
import org.genspectrum.lapis.silo.RhyDbAction
import org.genspectrum.lapis.silo.RhyDbClient
import org.genspectrum.lapis.silo.RhyDbQuery
import org.genspectrum.lapis.silo.SequenceType
import org.genspectrum.lapis.silo.True
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.stream.Stream

private val someMutationData = MutationData(
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
    insertedSymbols = "ABCD",
    position = 1234,
    sequenceName = "sequenceName",
)

class RhyDbQueryModelTest {
    @MockK
    lateinit var rhyDbClientMock: RhyDbClient

    @MockK
    lateinit var referenceGenomeSchemaMock: ReferenceGenomeSchema

    @MockK
    lateinit var rhyDbFilterExpressionMapperMock: RhyDbFilterExpressionMapper

    private lateinit var underTest: RhyDbQueryModel

    private val testDatabaseConfig = databaseConfig(
        metadata = listOf(
            DatabaseMetadata(name = "accession", type = MetadataType.STRING),
            DatabaseMetadata(name = "age", type = MetadataType.INT),
            DatabaseMetadata(name = "qc", type = MetadataType.FLOAT),
            DatabaseMetadata(name = "isBoolean", type = MetadataType.BOOLEAN),
            DatabaseMetadata(name = "date", type = MetadataType.DATE),
            DatabaseMetadata(name = "primaryKey", type = MetadataType.STRING),
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
        underTest = RhyDbQueryModel(
            rhyDbClient = rhyDbClientMock,
            rhyDbFilterExpressionMapper = rhyDbFilterExpressionMapperMock,
            referenceGenomeSchema = referenceGenomeSchemaMock,
            fastaHeaderTemplateParser = fastaHeaderTemplateParser,
            databaseConfig = testDatabaseConfig,
        )
    }

    @Test
    fun `aggregate calls the RHYDB client with an aggregated action`() {
        every { rhyDbClientMock.sendQuery(any<RhyDbQuery<AggregationData>>()) } returns Stream.empty()
        every { rhyDbFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True
        every { referenceGenomeSchemaMock.isSingleSegmented() } returns true

        underTest.getAggregated(
            AggregatedFiltersRequest(
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
            rhyDbClientMock.sendQuery(
                RhyDbQuery(RhyDbAction.aggregated(emptyList()), True),
            )
        }
    }

    @Test
    fun `GIVEN no fields specified THEN getDetails uses all metadata fields`() {
        every { rhyDbClientMock.sendQuery(any<RhyDbQuery<DetailsData>>()) } returns Stream.empty()
        every { rhyDbFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True

        underTest.getDetails(
            DetailsFiltersRequest(
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
            rhyDbClientMock.sendQuery(
                RhyDbQuery(
                    RhyDbAction.details(
                        listOf("accession", "age", "qc", "isBoolean", "date", "primaryKey"),
                    ),
                    True,
                ),
            )
        }
    }

    @Test
    fun `GIVEN fields specified THEN getDetails uses only those fields`() {
        every { rhyDbClientMock.sendQuery(any<RhyDbQuery<DetailsData>>()) } returns Stream.empty()
        every { rhyDbFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True

        underTest.getDetails(
            DetailsFiltersRequest(
                emptyMap(),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                listOf(PlainField("accession"), PlainField("date")),
                OrderBySpec.EMPTY,
            ),
        )

        verify {
            rhyDbClientMock.sendQuery(
                RhyDbQuery(
                    RhyDbAction.details(listOf("accession", "date")),
                    True,
                ),
            )
        }
    }

    @Test
    fun `computeNucleotideMutationProportions calls the RHYDB client with a mutations action`() {
        every { rhyDbClientMock.sendQuery(any<RhyDbQuery<MutationData>>()) } returns Stream.empty()
        every { rhyDbFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True
        every { referenceGenomeSchemaMock.isSingleSegmented() } returns true

        underTest.computeNucleotideMutationProportions(mutationProportionsRequest(minProportion = 0.5))

        verify {
            rhyDbClientMock.sendQuery(
                RhyDbQuery(RhyDbAction.mutations(0.5), True),
            )
        }
    }

    @Test
    fun `computeNucleotideMutationProportions ignores the segmentName if singleSegmentedSequenceFeature is enabled`() {
        every { rhyDbClientMock.sendQuery(any<RhyDbQuery<MutationData>>()) } returns Stream.of(someMutationData)
        every { rhyDbFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True
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
        every { rhyDbClientMock.sendQuery(any<RhyDbQuery<MutationData>>()) } returns
            Stream.of(mutationData(position = 123))
        every { rhyDbFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True
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
        every { rhyDbClientMock.sendQuery(any<RhyDbQuery<MutationData>>()) } returns Stream.of(
            mutationData(
                sequenceName = "sequenceName",
            ),
        )
        every { rhyDbFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True
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
            rhyDbClientMock.sendQuery(
                match<RhyDbQuery<MutationData>> {
                    when (val action = it.action) {
                        is RhyDbAction.MutationsAction -> action.fields.contains(MutationsField.SEQUENCE_NAME.value)
                        else -> false
                    }
                },
            )
        } returns Stream.of(
            mutationData(
                sequenceName = "sequenceName",
                position = 1234,
                mutationFrom = "A",
                mutationTo = "B",
            ),
        )
        every { rhyDbFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True
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
        every { rhyDbClientMock.sendQuery(any<RhyDbQuery<MutationData>>()) } returns Stream.of(someMutationData)
        every { rhyDbFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True
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
        every { rhyDbClientMock.sendQuery(any<RhyDbQuery<MutationData>>()) } returns Stream.of(someMutationData)
        every { rhyDbFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True

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
            rhyDbClientMock.sendQuery(
                match<RhyDbQuery<MutationData>> {
                    when (val action = it.action) {
                        is RhyDbAction.AminoAcidMutationsAction -> action.fields.contains(
                            MutationsField.SEQUENCE_NAME.value,
                        )

                        else -> false
                    }
                },
            )
        } returns Stream.of(
            mutationData(
                sequenceName = "sequenceName",
                position = 1234,
                mutationFrom = "A",
                mutationTo = "B",
            ),
        )
        every { rhyDbFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True

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
        every { rhyDbClientMock.sendQuery(any<RhyDbQuery<InsertionData>>()) } returns Stream.of(someInsertionData)
        every { rhyDbFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True
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
            insertion = "ins_1234:ABCD",
            count = 42,
            insertedSymbols = "ABCD",
            position = 1234,
            sequenceName = null,
        )
        assertThat(result, equalTo(listOf(expectedInsertion)))
    }

    @Test
    fun `getNucleotideInsertions includes the segment name if the nucleotide sequence has multiple segments`() {
        every { rhyDbClientMock.sendQuery(any<RhyDbQuery<InsertionData>>()) } returns Stream.of(someInsertionData)
        every { rhyDbFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True
        every { referenceGenomeSchemaMock.isSingleSegmented() } returns false

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
            sequenceName = "sequenceName",
        )
        assertThat(result, equalTo(listOf(expectedInsertion)))
    }

    @Test
    fun `GIVEN orderBy mutation WHEN computing nuc mutations THEN expands to component fields in order`() {
        val querySlot = slot<RhyDbQuery<MutationData>>()
        every { rhyDbClientMock.sendQuery(capture(querySlot)) } returns Stream.empty()
        every { rhyDbFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True
        every { referenceGenomeSchemaMock.isSingleSegmented() } returns true

        underTest.computeNucleotideMutationProportions(
            mutationProportionsRequest(
                orderByFields = listOf(OrderByField("mutation", Order.ASCENDING)).toOrderBySpec(),
            ),
        ).toList()

        val action = querySlot.captured.action as RhyDbAction.MutationsAction
        assertThat(
            action.orderByFields,
            equalTo(
                listOf(
                    OrderByField("sequenceName", Order.ASCENDING),
                    OrderByField("mutationFrom", Order.ASCENDING),
                    OrderByField("position", Order.ASCENDING),
                    OrderByField("mutationTo", Order.ASCENDING),
                ),
            ),
        )
    }

    @Test
    fun `GIVEN orderBy insertion WHEN computing nuc insertions THEN expands to component fields in order`() {
        val querySlot = slot<RhyDbQuery<InsertionData>>()
        every { rhyDbClientMock.sendQuery(capture(querySlot)) } returns Stream.empty()
        every { rhyDbFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True
        every { referenceGenomeSchemaMock.isSingleSegmented() } returns true

        underTest.getNucleotideInsertions(
            SequenceFiltersRequest(
                emptyMap(),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                listOf(OrderByField("insertion", Order.DESCENDING)).toOrderBySpec(),
            ),
        ).toList()

        val action = querySlot.captured.action as RhyDbAction.NucleotideInsertionsAction
        assertThat(
            action.orderByFields,
            equalTo(
                listOf(
                    OrderByField("sequenceName", Order.DESCENDING),
                    OrderByField("position", Order.DESCENDING),
                    OrderByField("insertedSymbols", Order.DESCENDING),
                ),
            ),
        )
    }

    @Test
    fun `getAminoAcidInsertions returns the sequenceName with the position`() {
        every { rhyDbClientMock.sendQuery(any<RhyDbQuery<InsertionData>>()) } returns Stream.of(someInsertionData)
        every { rhyDbFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True

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
    fun `getGenomicSequence calls the RHYDB client with a sequence action`() {
        every { rhyDbClientMock.sendQuery(any<RhyDbQuery<SequenceData>>()) } returns Stream.empty()
        every { rhyDbFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True
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
            rhyDbClientMock.sendQuery(
                RhyDbQuery(
                    RhyDbAction.genomicSequence(
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
        every { rhyDbClientMock.sendQuery(any<RhyDbQuery<SequenceData>>()) } returns Stream.empty()
        every { rhyDbFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True

        referenceGenomeSchemaMock = ReferenceGenomeSchema(
            nucleotideSequences = listOf(ReferenceSequenceSchema("Segment1"), ReferenceSequenceSchema("Segment2")),
            genes = emptyList(),
        )
        underTest = RhyDbQueryModel(
            rhyDbClient = rhyDbClientMock,
            rhyDbFilterExpressionMapper = rhyDbFilterExpressionMapperMock,
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
            rhyDbClientMock.sendQuery(
                RhyDbQuery(
                    RhyDbAction.genomicSequence(
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

    @Test
    fun `getAggregated splits plain and computed fields into groupByFields and computedFields`() {
        every { rhyDbClientMock.sendQuery(any<RhyDbQuery<AggregationData>>()) } returns Stream.empty()
        every { rhyDbFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True

        underTest.getAggregated(
            AggregatedFiltersRequest(
                emptyMap(),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                listOf(
                    PlainField("date"),
                    ComputedField("date", ScalarFunction.ISO_WEEK),
                ),
                OrderBySpec.EMPTY,
            ),
        )

        verify {
            rhyDbClientMock.sendQuery(
                RhyDbQuery(
                    RhyDbAction.aggregated(
                        groupByFields = listOf("date"),
                        computedFields = listOf(ComputedField("date", ScalarFunction.ISO_WEEK)),
                    ),
                    True,
                ),
            )
        }
    }

    @Test
    fun `getAggregated passes orderBy fields through unchanged for computed fields`() {
        val isoWeekField = ComputedField("date", ScalarFunction.ISO_WEEK)
        every { rhyDbFilterExpressionMapperMock.map(any<CommonSequenceFilters>()) } returns True
        every { rhyDbClientMock.sendQuery(any<RhyDbQuery<AggregationData>>()) } returns Stream.empty()

        val orderByFields = OrderBySpec.ByFields(
            listOf(OrderByField(field = isoWeekField.outputColumnName, order = Order.ASCENDING)),
        )

        underTest.getAggregated(
            AggregatedFiltersRequest(
                emptyMap(),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                listOf(isoWeekField),
                orderByFields,
            ),
        )

        verify {
            rhyDbClientMock.sendQuery(
                RhyDbQuery(
                    RhyDbAction.aggregated(
                        groupByFields = emptyList(),
                        computedFields = listOf(isoWeekField),
                        orderByFields = orderByFields,
                    ),
                    True,
                ),
            )
        }
    }
}
