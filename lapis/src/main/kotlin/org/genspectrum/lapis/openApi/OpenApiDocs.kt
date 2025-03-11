package org.genspectrum.lapis.openApi

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.DatabaseMetadata
import org.genspectrum.lapis.config.DatabaseSchema
import org.genspectrum.lapis.config.MetadataType
import org.genspectrum.lapis.config.OpennessLevel
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.config.ReferenceSequenceSchema
import org.genspectrum.lapis.config.SequenceFilterFieldName
import org.genspectrum.lapis.config.SequenceFilterFieldType
import org.genspectrum.lapis.config.SequenceFilterFields
import org.genspectrum.lapis.controller.AGGREGATED_GROUP_BY_FIELDS_DESCRIPTION
import org.genspectrum.lapis.controller.AMINO_ACID_MUTATION_DESCRIPTION
import org.genspectrum.lapis.controller.DATA_FORMAT_DESCRIPTION
import org.genspectrum.lapis.controller.DETAILS_FIELDS_DESCRIPTION
import org.genspectrum.lapis.controller.LIMIT_DESCRIPTION
import org.genspectrum.lapis.controller.NUCLEOTIDE_MUTATION_DESCRIPTION
import org.genspectrum.lapis.controller.OFFSET_DESCRIPTION
import org.genspectrum.lapis.controller.SEQUENCES_DATA_FORMAT_DESCRIPTION
import org.genspectrum.lapis.controller.middleware.Compression
import org.genspectrum.lapis.controller.middleware.DataFormat
import org.genspectrum.lapis.controller.middleware.SequencesDataFormat
import org.genspectrum.lapis.request.ACCESS_KEY_PROPERTY
import org.genspectrum.lapis.request.AMINO_ACID_INSERTIONS_PROPERTY
import org.genspectrum.lapis.request.AMINO_ACID_MUTATIONS_PROPERTY
import org.genspectrum.lapis.request.AminoAcidInsertion
import org.genspectrum.lapis.request.AminoAcidMutation
import org.genspectrum.lapis.request.COMPRESSION_PROPERTY
import org.genspectrum.lapis.request.DOWNLOAD_AS_FILE_PROPERTY
import org.genspectrum.lapis.request.DOWNLOAD_FILE_BASENAME_PROPERTY
import org.genspectrum.lapis.request.FIELDS_PROPERTY
import org.genspectrum.lapis.request.FORMAT_PROPERTY
import org.genspectrum.lapis.request.LIMIT_PROPERTY
import org.genspectrum.lapis.request.MIN_PROPORTION_PROPERTY
import org.genspectrum.lapis.request.NUCLEOTIDE_INSERTIONS_PROPERTY
import org.genspectrum.lapis.request.NUCLEOTIDE_MUTATIONS_PROPERTY
import org.genspectrum.lapis.request.NucleotideInsertion
import org.genspectrum.lapis.request.NucleotideMutation
import org.genspectrum.lapis.request.OFFSET_PROPERTY
import org.genspectrum.lapis.request.ORDER_BY_PROPERTY
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.response.COUNT_PROPERTY
import org.genspectrum.lapis.response.LapisInfo
import org.genspectrum.lapis.silo.ORDER_BY_RANDOM_FIELD_NAME

fun buildOpenApiSchema(
    sequenceFilterFields: SequenceFilterFields,
    databaseConfig: DatabaseConfig,
    referenceGenomeSchema: ReferenceGenomeSchema,
): OpenAPI =
    OpenAPI()
        .components(
            Components()
                .addSchemas(
                    PRIMITIVE_FIELD_FILTERS_SCHEMA,
                    Schema<String>()
                        .type("object")
                        .description("valid filters for sequence data")
                        .properties(computePrimitiveFieldFilters(databaseConfig, sequenceFilterFields)),
                )
                .addSchemas(
                    REQUEST_SCHEMA_WITH_MIN_PROPORTION,
                    Schema<String>()
                        .type("object")
                        .description("valid filters for sequence data")
                        .properties(
                            getSequenceFiltersWithFormat(
                                databaseConfig = databaseConfig,
                                sequenceFilterFields = sequenceFilterFields,
                                orderByFieldsSchema = mutationsOrderByFieldsEnum(),
                                dataFormatSchema = dataFormatSchema(),
                            ) + Pair(MIN_PROPORTION_PROPERTY, Schema<String>().type("number")),
                        ),
                )
                .addSchemas(
                    AGGREGATED_REQUEST_SCHEMA,
                    requestSchemaWithFields(
                        getSequenceFiltersWithFormat(
                            databaseConfig = databaseConfig,
                            sequenceFilterFields = sequenceFilterFields,
                            orderByFieldsSchema = aggregatedOrderByFieldsEnum(databaseConfig),
                            dataFormatSchema = dataFormatSchema(),
                        ),
                        AGGREGATED_GROUP_BY_FIELDS_DESCRIPTION,
                        databaseConfig.schema.metadata,
                    ),
                )
                .addSchemas(
                    DETAILS_REQUEST_SCHEMA,
                    requestSchemaWithFields(
                        getSequenceFiltersWithFormat(
                            databaseConfig = databaseConfig,
                            sequenceFilterFields = sequenceFilterFields,
                            orderByFieldsSchema = detailsOrderByFieldsEnum(databaseConfig),
                            dataFormatSchema = dataFormatSchema(),
                        ),
                        DETAILS_FIELDS_DESCRIPTION,
                        databaseConfig.schema.metadata,
                    ),
                )
                .addSchemas(
                    INSERTIONS_REQUEST_SCHEMA,
                    requestSchemaForCommonSequenceFilters(
                        getSequenceFiltersWithFormat(
                            databaseConfig = databaseConfig,
                            sequenceFilterFields = sequenceFilterFields,
                            orderByFieldsSchema = insertionsOrderByFieldsEnum(),
                            dataFormatSchema = dataFormatSchema(),
                        ),
                    ),
                )
                .addSchemas(
                    ALIGNED_AMINO_ACID_SEQUENCE_REQUEST_SCHEMA,
                    requestSchemaForCommonSequenceFilters(
                        getSequenceFiltersWithFormat(
                            databaseConfig = databaseConfig,
                            sequenceFilterFields = sequenceFilterFields,
                            orderByFieldsSchema = aminoAcidSequenceOrderByFieldsEnum(
                                referenceGenomeSchema,
                                databaseConfig,
                            ),
                            dataFormatSchema = sequencesFormatSchema(),
                        ),
                    ),
                )
                .addSchemas(
                    NUCLEOTIDE_SEQUENCE_REQUEST_SCHEMA,
                    requestSchemaForCommonSequenceFilters(
                        getSequenceFiltersWithFormat(
                            databaseConfig = databaseConfig,
                            sequenceFilterFields = sequenceFilterFields,
                            orderByFieldsSchema = nucleotideSequenceOrderByFieldsEnum(
                                referenceGenomeSchema,
                                databaseConfig,
                            ),
                            dataFormatSchema = sequencesFormatSchema(),
                        ),
                    ),
                )
                .addSchemas(
                    AGGREGATED_RESPONSE_SCHEMA,
                    lapisResponseSchema(
                        Schema<String>()
                            .type("object")
                            .description(
                                "Aggregated sequence data. " +
                                    "If fields are specified, then these fields are also keys in the result. " +
                                    "The key 'count' is always present.",
                            )
                            .required(listOf(COUNT_PROPERTY))
                            .properties(
                                getAggregatedResponseProperties(aggregatedMetadataFieldSchemas(databaseConfig)),
                            ),
                    ),
                )
                .addSchemas(
                    DETAILS_RESPONSE_SCHEMA,
                    lapisResponseSchema(
                        Schema<String>()
                            .type("object")
                            .description(
                                "The response contains the metadata of every sequence matching the sequence filters.",
                            )
                            .properties(detailsMetadataFieldSchemas(databaseConfig)),
                    ),
                )
                .addSchemas(
                    NUCLEOTIDE_MUTATIONS_RESPONSE_SCHEMA,
                    lapisResponseSchema(
                        Schema<String>()
                            .type("object")
                            .description("The count and proportion of a mutation.")
                            .properties(nucleotideMutationProportionSchema())
                            .required(
                                nucleotideMutationProportionSchema().keys
                                    .filterNot { referenceGenomeSchema.isSingleSegmented() && it == "sequenceName" },
                            ),
                    ),
                )
                .addSchemas(NUCLEOTIDE_MUTATIONS_SCHEMA, nucleotideMutations())
                .addSchemas(
                    AMINO_ACID_MUTATIONS_RESPONSE_SCHEMA,
                    lapisResponseSchema(
                        Schema<String>()
                            .type("object")
                            .description("The count and proportion of a mutation.")
                            .properties(aminoAcidMutationProportionSchema())
                            .required(aminoAcidMutationProportionSchema().keys.toList()),
                    ),
                )
                .addSchemas(
                    NUCLEOTIDE_INSERTIONS_RESPONSE_SCHEMA,
                    lapisResponseSchema(
                        Schema<String>()
                            .type("object")
                            .description("Nucleotide Insertion data.")
                            .properties(nucleotideInsertionSchema())
                            .required(
                                nucleotideInsertionSchema().keys
                                    .filterNot { referenceGenomeSchema.isSingleSegmented() && it == "sequenceName" },
                            ),
                    ),
                )
                .addSchemas(
                    AMINO_ACID_INSERTIONS_RESPONSE_SCHEMA,
                    lapisResponseSchema(
                        Schema<String>()
                            .type("object")
                            .description("Amino Acid Insertion data.")
                            .properties(aminoAcidInsertionSchema())
                            .required(aminoAcidInsertionSchema().keys.toList()),
                    ),
                )
                .addSchemas(
                    NUCLEOTIDE_SEQUENCES_RESPONSE_SCHEMA,
                    sequencesResponse(
                        databaseConfig.schema,
                        referenceGenomeSchema.nucleotideSequences,
                    ),
                )
                .addSchemas(
                    AMINO_ACID_SEQUENCES_RESPONSE_SCHEMA,
                    sequencesResponse(
                        databaseConfig.schema,
                        referenceGenomeSchema.genes,
                    ),
                )
                .addSchemas(FIELDS_TO_AGGREGATE_BY_SCHEMA, fieldsArray(databaseConfig.schema.metadata))
                .addSchemas(DETAILS_FIELDS_SCHEMA, fieldsArray(databaseConfig.schema.metadata))
                .addSchemas(AMINO_ACID_MUTATIONS_SCHEMA, aminoAcidMutations())
                .addSchemas(NUCLEOTIDE_INSERTIONS_SCHEMA, nucleotideInsertions())
                .addSchemas(AMINO_ACID_INSERTIONS_SCHEMA, aminoAcidInsertions())
                .addSchemas(
                    AGGREGATED_ORDER_BY_FIELDS_SCHEMA,
                    arraySchema(aggregatedOrderByFieldsEnum(databaseConfig)),
                )
                .addSchemas(DETAILS_ORDER_BY_FIELDS_SCHEMA, arraySchema(detailsOrderByFieldsEnum(databaseConfig)))
                .addSchemas(
                    MUTATIONS_ORDER_BY_FIELDS_SCHEMA,
                    arraySchema(mutationsOrderByFieldsEnum()),
                )
                .addSchemas(
                    INSERTIONS_ORDER_BY_FIELDS_SCHEMA,
                    arraySchema(insertionsOrderByFieldsEnum()),
                )
                .addSchemas(
                    AMINO_ACID_SEQUENCES_ORDER_BY_FIELDS_SCHEMA,
                    arraySchema(aminoAcidSequenceOrderByFieldsEnum(referenceGenomeSchema, databaseConfig)),
                )
                .addSchemas(
                    NUCLEOTIDE_SEQUENCES_ORDER_BY_FIELDS_SCHEMA,
                    arraySchema(nucleotideSequenceOrderByFieldsEnum(referenceGenomeSchema, databaseConfig)),
                )
                .addSchemas(
                    SEGMENT_SCHEMA,
                    fieldsEnum(additionalFields = referenceGenomeSchema.nucleotideSequences.map { it.name }),
                )
                .addSchemas(GENE_SCHEMA, fieldsEnum(additionalFields = referenceGenomeSchema.genes.map { it.name }))
                .addSchemas(LIMIT_SCHEMA, limitSchema())
                .addSchemas(OFFSET_SCHEMA, offsetSchema())
                .addSchemas(SEQUENCES_FORMAT_SCHEMA, sequencesFormatSchema())
                .addSchemas(FORMAT_SCHEMA, dataFormatSchema()),
        )

private fun getSequenceFiltersWithFormat(
    databaseConfig: DatabaseConfig,
    sequenceFilterFields: SequenceFilterFields,
    orderByFieldsSchema: Schema<Any>,
    dataFormatSchema: Schema<*>,
): Map<SequenceFilterFieldName, Schema<*>> =
    getSequenceFilters(databaseConfig, sequenceFilterFields, orderByFieldsSchema) +
        Pair(FORMAT_PROPERTY, dataFormatSchema)

private fun getSequenceFilters(
    databaseConfig: DatabaseConfig,
    sequenceFilterFields: SequenceFilterFields,
    orderByFieldsSchema: Schema<*>,
): Map<SequenceFilterFieldName, Schema<*>> =
    computePrimitiveFieldFilters(databaseConfig, sequenceFilterFields) +
        Pair(NUCLEOTIDE_MUTATIONS_PROPERTY, nucleotideMutations()) +
        Pair(AMINO_ACID_MUTATIONS_PROPERTY, aminoAcidMutations()) +
        Pair(NUCLEOTIDE_INSERTIONS_PROPERTY, nucleotideInsertions()) +
        Pair(AMINO_ACID_INSERTIONS_PROPERTY, aminoAcidInsertions()) +
        Pair(ORDER_BY_PROPERTY, orderByPostSchema(orderByFieldsSchema)) +
        Pair(LIMIT_PROPERTY, limitSchema()) +
        Pair(OFFSET_PROPERTY, offsetSchema()) +
        Pair(DOWNLOAD_AS_FILE_PROPERTY, downloadAsFileSchema()) +
        Pair(DOWNLOAD_FILE_BASENAME_PROPERTY, downloadFileBasenameSchema()) +
        Pair(COMPRESSION_PROPERTY, compressionSchema())

fun downloadAsFileSchema(): Schema<*> =
    BooleanSchema()
        ._default(false)
        .description(DOWNLOAD_AS_FILE_DESCRIPTION)

fun downloadFileBasenameSchema(): Schema<*> =
    StringSchema()
        .description(DOWNLOAD_FILE_BASENAME_DESCRIPTION)

fun compressionSchema(): Schema<*> =
    StringSchema()
        .description(COMPRESSION_DESCRIPTION)
        ._enum(Compression.entries.map { it.value })

private fun computePrimitiveFieldFilters(
    databaseConfig: DatabaseConfig,
    sequenceFilterFields: SequenceFilterFields,
): Map<SequenceFilterFieldName, Schema<Any>> =
    when (databaseConfig.schema.opennessLevel) {
        OpennessLevel.PROTECTED -> primitiveSequenceFilterFieldSchemas(sequenceFilterFields) +
            (ACCESS_KEY_PROPERTY to accessKeySchema())

        else -> primitiveSequenceFilterFieldSchemas(sequenceFilterFields)
    }

private fun lapisResponseSchema(dataSchema: Schema<Any>) =
    Schema<Any>().type("object")
        .properties(
            mapOf(
                "data" to Schema<Any>().type("array").items(dataSchema),
                "info" to infoResponseSchema(),
            ),
        )
        .required(listOf("data", "info"))

private fun infoResponseSchema() =
    Schema<LapisInfo>().type("object")
        .description(LAPIS_INFO_DESCRIPTION)
        .properties(
            mapOf(
                "dataVersion" to Schema<String>().type("string")
                    .description(LAPIS_DATA_VERSION_RESPONSE_DESCRIPTION)
                    .example(LAPIS_DATA_VERSION_EXAMPLE),
                "requestId" to Schema<String>().type("string").description(REQUEST_ID_HEADER_DESCRIPTION),
                "requestInfo" to Schema<String>().type("string").description(REQUEST_INFO_STRING_DESCRIPTION),
                "reportTo" to Schema<String>().type("string"),
                "lapisVersion" to StringSchema().description(VERSION_DESCRIPTION),
                "siloVersion" to StringSchema().description(SILO_VERSION_DESCRIPTION),
            ),
        )
        .required(listOf("reportTo"))

private fun aggregatedMetadataFieldSchemas(databaseConfig: DatabaseConfig) =
    databaseConfig.schema.metadata.associate { it.name to Schema<String>().type(mapToOpenApiType(it.type)) }

private fun detailsMetadataFieldSchemas(databaseConfig: DatabaseConfig) =
    databaseConfig.schema
        .metadata
        .associate { it.name to Schema<String>().type(mapToOpenApiType(it.type)) }

private fun mapToOpenApiType(type: MetadataType): String =
    when (type) {
        MetadataType.STRING -> "string"
        MetadataType.DATE -> "string"
        MetadataType.INT -> "integer"
        MetadataType.FLOAT -> "number"
        MetadataType.BOOLEAN -> "boolean"
    }

private fun primitiveSequenceFilterFieldSchemas(sequenceFilterFields: SequenceFilterFields) =
    sequenceFilterFields.fields
        .values
        .associate { (fieldName, field) -> fieldName to filterFieldSchema(field) }

private fun filterFieldSchema(fieldType: SequenceFilterFieldType) =
    when (fieldType) {
        SequenceFilterFieldType.String ->
            Schema<String>().anyOf(
                listOf(
                    nullableStringSchema(fieldType.openApiType),
                    logicalOrArraySchema(nullableStringSchema(fieldType.openApiType)),
                ),
            )

        SequenceFilterFieldType.Lineage -> {
            val fieldSchema = nullableStringSchema(fieldType.openApiType)
                .description(
                    "Filter sequences by this lineage. " +
                        "You can suffix the filter value with '*' to include sublineages.",
                )
            Schema<String>().anyOf(
                listOf(
                    fieldSchema,
                    logicalOrArraySchema(fieldSchema),
                ),
            )
        }

        is SequenceFilterFieldType.StringSearch ->
            Schema<String>().anyOf(
                listOf(
                    nullableStringRegexSchema(fieldType.associatedField),
                    logicalOrArraySchema(nullableStringRegexSchema(fieldType.associatedField)),
                ),
            )

        else -> nullableStringSchema(fieldType.openApiType)
    }

private fun nullableStringRegexSchema(associatedField: SequenceFilterFieldName) =
    nullableStringSchema("string")
        .description(
            "A regex pattern (subset of PCRE) for filtering '$associatedField'. " +
                "For details on the syntax, see https://github.com/google/re2/wiki/Syntax.",
        )

private fun nullableStringSchema(type: String) = Schema<String>().type(type).nullable(true)

private fun requestSchemaForCommonSequenceFilters(
    requestProperties: Map<SequenceFilterFieldName, Schema<out Any>>,
): Schema<*> =
    Schema<String>()
        .type("object")
        .description("valid filters for sequence data")
        .properties(requestProperties)

private fun requestSchemaWithFields(
    requestProperties: Map<SequenceFilterFieldName, Schema<out Any>>,
    fieldsDescription: String,
    databaseConfig: List<DatabaseMetadata>,
): Schema<*> =
    Schema<String>()
        .type("object")
        .description("valid filters for sequence data")
        .properties(
            requestProperties + Pair(
                FIELDS_PROPERTY,
                fieldsArray(databaseConfig).description(fieldsDescription),
            ),
        )

private fun getAggregatedResponseProperties(filterProperties: Map<SequenceFilterFieldName, Schema<Any>>) =
    filterProperties.mapValues { (_, schema) ->
        schema.description(
            "This field is present if and only if it was specified in \"fields\" in the request. " +
                "The response is stratified by this field.",
        )
    } + mapOf(
        COUNT_PROPERTY to IntegerSchema().description("The number of sequences matching the filters."),
    )

fun accessKeySchema(): Schema<Any> = StringSchema().description(ACCESS_KEY_DESCRIPTION)

private fun nucleotideMutationProportionSchema() =
    mapOf(
        "mutation" to StringSchema()
            .example("sequence1:G29741T")
            .description(
                "If the genome only contains one segment then this is: " +
                    "(mutationFrom)(position)(mutationTo)." +
                    "If it has more than one segment (e.g., influenza), then the sequence is contained here: " +
                    "(sequenceName):(mutationFrom)(position)" +
                    "(mutationTo)",
            ),
        "proportion" to NumberSchema()
            .example(0.54321)
            .description(
                "Number of sequences with this mutation divided by the total number sequences matching the " +
                    "given filter criteria with non-ambiguous reads at that position (i.e. count/coverage).",
            ),
        "count" to IntegerSchema()
            .example(1234)
            .description("Total number of sequences with this mutation matching the given sequence filter criteria"),
        "coverage" to IntegerSchema()
            .example(2345)
            .description(
                "Total number of sequences with non-ambiguous reads matching the given sequence filter criteria",
            ),
        "sequenceName" to StringSchema()
            .example("sequence1")
            .description(
                "The name of the segment in which the mutation occurs. Null if the genome is single-segmented.",
            ),
        "mutationFrom" to StringSchema()
            .example("G")
            .description("The nucleotide symbol in the reference genome at the position of the mutation"),
        "mutationTo" to StringSchema()
            .example("T")
            .description("The nucleotide symbol that the mutation changes to or '-' in case of a deletion"),
        "position" to IntegerSchema()
            .example(29741)
            .description("The position in the reference genome where the mutation occurs"),
    )

private fun aminoAcidMutationProportionSchema() =
    mapOf(
        "mutation" to StringSchema()
            .example("ORF1a:G29741T")
            .description("Of the format (sequenceName):(mutationFrom)(position)(mutationTo)"),
        "proportion" to NumberSchema()
            .example(0.54321)
            .description(
                "Number of sequences with this mutation divided by the total number sequences matching the " +
                    "given filter criteria with non-ambiguous reads at that position (i.e. count/coverage).",
            ),
        "count" to IntegerSchema()
            .example(42)
            .description("Total number of sequences with this mutation matching the given sequence filter criteria"),
        "coverage" to IntegerSchema()
            .example(2345)
            .description(
                "Total number of sequences with non-ambiguous reads matching the given sequence filter criteria",
            ),
        "sequenceName" to StringSchema()
            .example("ORF1a")
            .description("The name of the gene in which the mutation occurs."),
        "mutationFrom" to StringSchema()
            .example("G")
            .description("The amino acid symbol in the reference genome at the position of the mutation"),
        "mutationTo" to StringSchema()
            .example("T")
            .description("The amino acid symbol that the mutation changes to or '-' in case of a deletion"),
        "position" to IntegerSchema()
            .example(29741)
            .description("The position in the reference genome where the mutation occurs"),
    )

private fun nucleotideInsertionSchema() =
    mapOf(
        "insertion" to StringSchema()
            .example("ins_segment1:22204:CAGAAG")
            .description(
                "A nucleotide insertion in the format \"ins_(segment):(position):(insertedSymbols)\".  " +
                    "If the pathogen has only one segment LAPIS will omit the segment name (\"ins_22204:CAGAAG\").",
            ),
        "count" to IntegerSchema()
            .example(42)
            .description("Total number of sequences with this insertion matching the given sequence filter criteria"),
        "insertedSymbols" to StringSchema()
            .example("CAGAAG")
            .description("The nucleotide symbols that were inserted at the given position"),
        "position" to IntegerSchema()
            .example(22204)
            .description("The position in the reference genome where the insertion occurs"),
        "sequenceName" to StringSchema()
            .example("segment1")
            .description(
                "The name of the segment in which the insertion occurs. Null if the genome is single-segmented.",
            ),
    )

private fun aminoAcidInsertionSchema() =
    mapOf(
        "insertion" to StringSchema()
            .example("ins_ORF1a:22204:CAGAAG")
            .description("An amino acid insertion in the format \"ins_(gene):(position):(insertedSymbols)\"."),
        "count" to IntegerSchema()
            .description("Total number of sequences with this insertion matching the given sequence filter criteria."),
        "insertedSymbols" to StringSchema()
            .example("CAGAAG")
            .description("The amino acid symbols that were inserted at the given position."),
        "position" to IntegerSchema()
            .example(22204)
            .description("The position in the reference genome where the insertion occurs."),
        "sequenceName" to StringSchema()
            .example("ORF1a")
            .description("The name of the gene in which the insertion occurs."),
    )

private fun nucleotideMutations() =
    Schema<List<NucleotideMutation>>()
        .type("array")
        .description("Logical \"and\" concatenation of a list of mutations.")
        .items(
            Schema<String>()
                .type("string")
                .example("sequence1:A123T")
                .description(NUCLEOTIDE_MUTATION_DESCRIPTION),
        )

private fun aminoAcidMutations() =
    Schema<List<AminoAcidMutation>>()
        .type("array")
        .description("Logical \"and\" concatenation of a list of mutations.")
        .items(
            Schema<String>()
                .type("string")
                .example("S:123T")
                .description(AMINO_ACID_MUTATION_DESCRIPTION),
        )

private fun nucleotideInsertions() =
    Schema<List<NucleotideInsertion>>()
        .type("array")
        .description("Logical \"and\" concatenation of a list of insertions.")
        .items(
            Schema<String>()
                .type("string")
                .example("ins_123:ATT")
                .description(
                    """
                    |A nucleotide insertion in the format "ins_(\<sequenceName\>:)?\<position\>:\<insertion\>".  
                    |If the sequenceName is not provided, LAPIS will use the default sequence name.  
                    """.trimMargin(),
                ),
        )

private fun aminoAcidInsertions() =
    Schema<List<AminoAcidInsertion>>()
        .type("array")
        .description("Logical \"and\" concatenation of a list of insertions.")
        .items(
            Schema<String>()
                .type("string")
                .example("ins_ORF1a:123:ATT")
                .description(
                    """
                    |An amino acid insertion in the format "ins_\<gene\>:\<position\>:\<insertion\>".  
                    """.trimMargin(),
                ),
        )

private fun orderByPostSchema(orderByFieldsSchema: Schema<*>) =
    Schema<List<String>>()
        .type("array")
        .items(
            Schema<String>().anyOf(
                listOf(
                    orderByFieldsSchema,
                    Schema<OrderByField>()
                        .type("object")
                        .description("The fields by which the result is ordered with ascending or descending order.")
                        .required(listOf("field"))
                        .properties(
                            mapOf(
                                "field" to orderByFieldsSchema,
                                "type" to Schema<String>()
                                    .type("string")
                                    ._enum(listOf("ascending", "descending"))
                                    ._default("ascending"),
                            ),
                        ),
                ),
            ),
        )

private fun limitSchema() =
    Schema<Int>()
        .type("integer")
        .description(LIMIT_DESCRIPTION)
        .example(100)

private fun offsetSchema() =
    Schema<Int>()
        .type("integer")
        .description(OFFSET_DESCRIPTION)

private fun dataFormatSchema() =
    Schema<String>()
        .type("string")
        .description(
            DATA_FORMAT_DESCRIPTION,
        )
        ._enum(listOf(DataFormat.JSON, DataFormat.CSV, DataFormat.CSV_WITHOUT_HEADERS, DataFormat.TSV))
        ._default(DataFormat.JSON)

private fun sequencesFormatSchema() =
    Schema<String>()
        .type("string")
        .description(SEQUENCES_DATA_FORMAT_DESCRIPTION)
        ._enum(listOf(SequencesDataFormat.FASTA, SequencesDataFormat.JSON, SequencesDataFormat.NDJSON))

private fun fieldsArray(
    databaseConfig: List<DatabaseMetadata>,
    additionalFields: List<String> = emptyList(),
) = arraySchema(fieldsEnum(databaseConfig, additionalFields))

private fun aggregatedOrderByFieldsEnum(databaseConfig: DatabaseConfig) =
    orderByFieldsEnum(databaseConfig.schema.metadata, listOf("count"))

private fun mutationsOrderByFieldsEnum() =
    orderByFieldsEnum(
        emptyList(),
        listOf("mutation", "count", "proportion", "sequenceName", "mutationFrom", "mutationTo", "position"),
    )

private fun insertionsOrderByFieldsEnum() =
    orderByFieldsEnum(emptyList(), listOf("insertion", "count", "position", "sequenceName", "insertedSymbols"))

private fun aminoAcidSequenceOrderByFieldsEnum(
    referenceGenomeSchema: ReferenceGenomeSchema,
    databaseConfig: DatabaseConfig,
) = orderByFieldsEnum(emptyList(), referenceGenomeSchema.genes.map { it.name } + databaseConfig.schema.primaryKey)

private fun nucleotideSequenceOrderByFieldsEnum(
    referenceGenomeSchema: ReferenceGenomeSchema,
    databaseConfig: DatabaseConfig,
) = orderByFieldsEnum(
    emptyList(),
    referenceGenomeSchema.nucleotideSequences.map {
        it.name
    } + databaseConfig.schema.primaryKey,
)

private fun detailsOrderByFieldsEnum(databaseConfig: DatabaseConfig) = orderByFieldsEnum(databaseConfig.schema.metadata)

private fun orderByFieldsEnum(
    databaseConfig: List<DatabaseMetadata> = emptyList(),
    additionalFields: List<String> = emptyList(),
) = fieldsEnum(databaseConfig, additionalFields + ORDER_BY_RANDOM_FIELD_NAME)

private fun fieldsEnum(
    databaseConfig: List<DatabaseMetadata> = emptyList(),
    additionalFields: List<String> = emptyList(),
) = Schema<String>()
    .type("string")
    ._enum(databaseConfig.map { it.name } + additionalFields)

private fun logicalOrArraySchema(schema: Schema<Any>) =
    arraySchema(schema)
        .description("Logical \"or\" concatenation of a list of values.")

private fun arraySchema(schema: Schema<Any>) =
    ArraySchema()
        .items(schema)

private fun sequencesResponse(
    schema: DatabaseSchema,
    referenceSequenceSchemas: List<ReferenceSequenceSchema>,
): Schema<*> {
    val baseSchema = Schema<Any>()
        .type("object")
        .addProperty(schema.primaryKey, Schema<String>().type("string"))
        .addRequiredItem(schema.primaryKey)

    return when (referenceSequenceSchemas.size == 1) {
        true ->
            baseSchema
                .addProperty(
                    referenceSequenceSchemas[0].name,
                    Schema<String>()
                        .type("string")
                        .description("The sequence data."),
                )
                .addRequiredItem(referenceSequenceSchemas[0].name)
                .description("An object containing the primary key and the requested sequence of a sample.")

        false -> {
            for (nucleotideSequence in referenceSequenceSchemas) {
                baseSchema.addProperty(nucleotideSequence.name, Schema<String>().type("string"))
            }
            baseSchema
                .description(
                    "An object containing the primary key and the requested sequence of a sample. " +
                        "This object always contains two exactly keys. " +
                        "Only the requested sequence is contained in the response, the others are omitted.",
                )
        }
    }
}
