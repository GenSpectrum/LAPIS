package org.genspectrum.lapis.openApi

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.DatabaseMetadata
import org.genspectrum.lapis.config.MetadataType
import org.genspectrum.lapis.config.OpennessLevel
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.config.SequenceFilterFieldName
import org.genspectrum.lapis.config.SequenceFilterFieldType
import org.genspectrum.lapis.config.SequenceFilterFields
import org.genspectrum.lapis.controller.ACCESS_KEY_PROPERTY
import org.genspectrum.lapis.controller.AGGREGATED_GROUP_BY_FIELDS_DESCRIPTION
import org.genspectrum.lapis.controller.AMINO_ACID_INSERTIONS_PROPERTY
import org.genspectrum.lapis.controller.AMINO_ACID_MUTATIONS_PROPERTY
import org.genspectrum.lapis.controller.AMINO_ACID_MUTATION_DESCRIPTION
import org.genspectrum.lapis.controller.COMPRESSION_PROPERTY
import org.genspectrum.lapis.controller.Compression
import org.genspectrum.lapis.controller.DETAILS_FIELDS_DESCRIPTION
import org.genspectrum.lapis.controller.DOWNLOAD_AS_FILE_PROPERTY
import org.genspectrum.lapis.controller.DataFormat
import org.genspectrum.lapis.controller.FIELDS_PROPERTY
import org.genspectrum.lapis.controller.FORMAT_DESCRIPTION
import org.genspectrum.lapis.controller.FORMAT_PROPERTY
import org.genspectrum.lapis.controller.LIMIT_DESCRIPTION
import org.genspectrum.lapis.controller.LIMIT_PROPERTY
import org.genspectrum.lapis.controller.MIN_PROPORTION_PROPERTY
import org.genspectrum.lapis.controller.NUCLEOTIDE_INSERTIONS_PROPERTY
import org.genspectrum.lapis.controller.NUCLEOTIDE_MUTATIONS_PROPERTY
import org.genspectrum.lapis.controller.NUCLEOTIDE_MUTATION_DESCRIPTION
import org.genspectrum.lapis.controller.OFFSET_DESCRIPTION
import org.genspectrum.lapis.controller.OFFSET_PROPERTY
import org.genspectrum.lapis.controller.ORDER_BY_PROPERTY
import org.genspectrum.lapis.request.AminoAcidInsertion
import org.genspectrum.lapis.request.AminoAcidMutation
import org.genspectrum.lapis.request.LapisInfo
import org.genspectrum.lapis.request.NucleotideInsertion
import org.genspectrum.lapis.request.NucleotideMutation
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.response.COUNT_PROPERTY
import org.genspectrum.lapis.silo.ORDER_BY_RANDOM_FIELD_NAME

fun buildOpenApiSchema(
    sequenceFilterFields: SequenceFilterFields,
    databaseConfig: DatabaseConfig,
    referenceGenomeSchema: ReferenceGenomeSchema,
): OpenAPI {
    return OpenAPI()
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
                                databaseConfig,
                                sequenceFilterFields,
                                mutationsOrderByFieldsEnum(),
                            ) + Pair(MIN_PROPORTION_PROPERTY, Schema<String>().type("number")),
                        ),
                )
                .addSchemas(
                    AGGREGATED_REQUEST_SCHEMA,
                    requestSchemaWithFields(
                        getSequenceFiltersWithFormat(
                            databaseConfig,
                            sequenceFilterFields,
                            aggregatedOrderByFieldsEnum(databaseConfig),
                        ),
                        AGGREGATED_GROUP_BY_FIELDS_DESCRIPTION,
                        databaseConfig.schema.metadata,
                    ),
                )
                .addSchemas(
                    DETAILS_REQUEST_SCHEMA,
                    requestSchemaWithFields(
                        getSequenceFiltersWithFormat(
                            databaseConfig,
                            sequenceFilterFields,
                            detailsOrderByFieldsEnum(databaseConfig),
                        ),
                        DETAILS_FIELDS_DESCRIPTION,
                        databaseConfig.schema.metadata,
                    ),
                )
                .addSchemas(
                    INSERTIONS_REQUEST_SCHEMA,
                    requestSchemaForCommonSequenceFilters(
                        getSequenceFiltersWithFormat(
                            databaseConfig,
                            sequenceFilterFields,
                            insertionsOrderByFieldsEnum(),
                        ),
                    ),
                )
                .addSchemas(
                    ALIGNED_AMINO_ACID_SEQUENCE_REQUEST_SCHEMA,
                    requestSchemaForCommonSequenceFilters(
                        getSequenceFilters(
                            databaseConfig,
                            sequenceFilterFields,
                            aminoAcidSequenceOrderByFieldsEnum(referenceGenomeSchema, databaseConfig),
                        ),
                    ),
                )
                .addSchemas(
                    NUCLEOTIDE_SEQUENCE_REQUEST_SCHEMA,
                    requestSchemaForCommonSequenceFilters(
                        getSequenceFilters(
                            databaseConfig,
                            sequenceFilterFields,
                            nucleotideSequenceOrderByFieldsEnum(referenceGenomeSchema, databaseConfig),
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
                            .description(
                                "The response contains the metadata of every sequence matching the sequence filters.",
                            )
                            .properties(nucleotideMutationProportionSchema()),
                    ),
                )
                .addSchemas(NUCLEOTIDE_MUTATIONS_SCHEMA, nucleotideMutations())
                .addSchemas(
                    AMINO_ACID_MUTATIONS_RESPONSE_SCHEMA,
                    lapisResponseSchema(
                        Schema<String>()
                            .type("object")
                            .description(
                                "The response contains the metadata of every sequence matching the sequence filters.",
                            )
                            .properties(aminoAcidMutationProportionSchema()),
                    ),
                )
                .addSchemas(
                    NUCLEOTIDE_INSERTIONS_RESPONSE_SCHEMA,
                    lapisResponseSchema(
                        Schema<String>()
                            .type("object")
                            .description("Nucleotide Insertion data.")
                            .properties(nucleotideInsertionSchema()),
                    ),
                )
                .addSchemas(
                    AMINO_ACID_INSERTIONS_RESPONSE_SCHEMA,
                    lapisResponseSchema(
                        Schema<String>()
                            .type("object")
                            .description("Amino Acid Insertion data.")
                            .properties(aminoAcidInsertionSchema()),
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
                .addSchemas(FORMAT_SCHEMA, formatSchema()),
        )
}

private fun getSequenceFiltersWithFormat(
    databaseConfig: DatabaseConfig,
    sequenceFilterFields: SequenceFilterFields,
    orderByFieldsSchema: Schema<Any>,
): Map<SequenceFilterFieldName, Schema<*>> =
    getSequenceFilters(databaseConfig, sequenceFilterFields, orderByFieldsSchema) +
        Pair(FORMAT_PROPERTY, formatSchema())

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
        Pair(COMPRESSION_PROPERTY, compressionSchema())

fun downloadAsFileSchema(): Schema<*> =
    BooleanSchema()
        ._default(false)
        .description(DOWNLOAD_AS_FILE_DESCRIPTION)

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
            ),
        )
        .required(listOf("dataVersion"))

private fun aggregatedMetadataFieldSchemas(databaseConfig: DatabaseConfig) =
    databaseConfig.schema.metadata.associate { it.name to Schema<String>().type(mapToOpenApiType(it.type)) }

private fun detailsMetadataFieldSchemas(databaseConfig: DatabaseConfig) =
    databaseConfig.schema.metadata.filter {
        it.type != MetadataType.AMINO_ACID_INSERTION && it.type != MetadataType.NUCLEOTIDE_INSERTION
    }
        .associate { it.name to Schema<String>().type(mapToOpenApiType(it.type)) }

private fun mapToOpenApiType(type: MetadataType): String =
    when (type) {
        MetadataType.STRING -> "string"
        MetadataType.PANGO_LINEAGE -> "string"
        MetadataType.DATE -> "string"
        MetadataType.INT -> "integer"
        MetadataType.FLOAT -> "number"
        MetadataType.NUCLEOTIDE_INSERTION -> "string"
        MetadataType.AMINO_ACID_INSERTION -> "string"
    }

private fun primitiveSequenceFilterFieldSchemas(sequenceFilterFields: SequenceFilterFields) =
    sequenceFilterFields.fields
        .values
        .associate { (fieldName, field) -> fieldName to filterFieldSchema(field) }

private fun filterFieldSchema(fieldType: SequenceFilterFieldType) =
    when (fieldType) {
        SequenceFilterFieldType.String, SequenceFilterFieldType.PangoLineage ->
            Schema<String>().anyOf(
                listOf(
                    Schema<String>().type(fieldType.openApiType),
                    arraySchema(Schema<String>().type(fieldType.openApiType)),
                ),
            )

        else -> Schema<String>().type(fieldType.openApiType)
    }

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

fun accessKeySchema() = StringSchema().description(ACCESS_KEY_DESCRIPTION)

private fun nucleotideMutationProportionSchema() =
    mapOf(
        "mutation" to Schema<String>().type("string").example("T123C").description("The mutation that was found."),
        "proportion" to Schema<String>().type("number").description("The proportion of sequences having the mutation."),
        "count" to Schema<String>().type("integer")
            .description("The number of sequences matching having the mutation."),
    )

private fun aminoAcidMutationProportionSchema() =
    mapOf(
        "mutation" to Schema<String>().type("string").example("ORF1a:123").description(
            "A amino acid mutation that was found in the format \"\\<gene\\>:\\<position\\>",
        ),
        "proportion" to Schema<String>().type("number").description("The proportion of sequences having the mutation."),
        "count" to Schema<String>().type("integer")
            .description("The number of sequences matching having the mutation."),
    )

private fun nucleotideInsertionSchema() =
    mapOf(
        "insertion" to Schema<String>().type("string")
            .example("ins_segment:123:AAT")
            .description("The insertion that was found."),
        "count" to Schema<String>().type("integer")
            .description("The number of sequences matching having the insertion."),
    )

private fun aminoAcidInsertionSchema() =
    mapOf(
        "insertion" to Schema<String>().type("string")
            .example("ins_gene:123:AAT")
            .description("The insertion that was found."),
        "count" to Schema<String>().type("integer")
            .description("The number of sequences matching having the insertion."),
    )

private fun nucleotideMutations() =
    Schema<List<NucleotideMutation>>()
        .type("array")
        .description(NUCLEOTIDE_MUTATION_DESCRIPTION)
        .items(
            Schema<String>()
                .type("string")
                .example("sequence1:A123T")
                .description(NUCLEOTIDE_MUTATION_DESCRIPTION),
        )

private fun aminoAcidMutations() =
    Schema<List<AminoAcidMutation>>()
        .type("array")
        .items(
            Schema<String>()
                .type("string")
                .example("S:123T")
                .description(AMINO_ACID_MUTATION_DESCRIPTION),
        )

private fun nucleotideInsertions() =
    Schema<List<NucleotideInsertion>>()
        .type("array")
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
        .items(
            Schema<String>()
                .type("string")
                .example("ins_ORF1:123:ATT")
                .description(
                    """
                    |A amino acid insertion in the format "ins_\<gene\>:\<position\>:\<insertion\>".  
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

private fun formatSchema() =
    Schema<String>()
        .type("string")
        .description(
            FORMAT_DESCRIPTION,
        )
        ._enum(listOf(DataFormat.JSON, DataFormat.CSV, DataFormat.CSV_WITHOUT_HEADERS, DataFormat.TSV))
        ._default(DataFormat.JSON)

private fun fieldsArray(
    databaseConfig: List<DatabaseMetadata>,
    additionalFields: List<String> = emptyList(),
) = arraySchema(fieldsEnum(databaseConfig, additionalFields))

private fun aggregatedOrderByFieldsEnum(databaseConfig: DatabaseConfig) =
    orderByFieldsEnum(databaseConfig.schema.metadata, listOf("count"))

private fun mutationsOrderByFieldsEnum() = orderByFieldsEnum(emptyList(), listOf("mutation", "count", "proportion"))

private fun insertionsOrderByFieldsEnum() = orderByFieldsEnum(emptyList(), listOf("insertion", "count"))

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

private fun arraySchema(schema: Schema<Any>) =
    ArraySchema()
        .items(schema)
