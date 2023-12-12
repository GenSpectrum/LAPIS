package org.genspectrum.lapis.openApi

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.DatabaseMetadata
import org.genspectrum.lapis.config.MetadataType
import org.genspectrum.lapis.config.OpennessLevel
import org.genspectrum.lapis.config.ReferenceGenome
import org.genspectrum.lapis.config.SequenceFilterFieldName
import org.genspectrum.lapis.config.SequenceFilterFields
import org.genspectrum.lapis.controller.AGGREGATED_GROUP_BY_FIELDS_DESCRIPTION
import org.genspectrum.lapis.controller.AMINO_ACID_INSERTIONS_PROPERTY
import org.genspectrum.lapis.controller.AMINO_ACID_MUTATIONS_PROPERTY
import org.genspectrum.lapis.controller.DETAILS_FIELDS_DESCRIPTION
import org.genspectrum.lapis.controller.FIELDS_PROPERTY
import org.genspectrum.lapis.controller.FORMAT_DESCRIPTION
import org.genspectrum.lapis.controller.FORMAT_PROPERTY
import org.genspectrum.lapis.controller.LIMIT_DESCRIPTION
import org.genspectrum.lapis.controller.LIMIT_PROPERTY
import org.genspectrum.lapis.controller.MIN_PROPORTION_PROPERTY
import org.genspectrum.lapis.controller.NUCLEOTIDE_INSERTIONS_PROPERTY
import org.genspectrum.lapis.controller.NUCLEOTIDE_MUTATIONS_PROPERTY
import org.genspectrum.lapis.controller.OFFSET_DESCRIPTION
import org.genspectrum.lapis.controller.OFFSET_PROPERTY
import org.genspectrum.lapis.controller.ORDER_BY_PROPERTY
import org.genspectrum.lapis.request.AminoAcidInsertion
import org.genspectrum.lapis.request.AminoAcidMutation
import org.genspectrum.lapis.request.LAPIS_DATA_VERSION_HEADER
import org.genspectrum.lapis.request.NucleotideInsertion
import org.genspectrum.lapis.request.NucleotideMutation
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.response.COUNT_PROPERTY

fun buildOpenApiSchema(
    sequenceFilterFields: SequenceFilterFields,
    databaseConfig: DatabaseConfig,
    referenceGenome: ReferenceGenome,
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
                            fieldsEnum(databaseConfig.schema.metadata),
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
                            aminoAcidSequenceFieldsEnum(referenceGenome, databaseConfig),
                        ),
                    ),
                )
                .addSchemas(
                    NUCLEOTIDE_SEQUENCE_REQUEST_SCHEMA,
                    requestSchemaForCommonSequenceFilters(
                        getSequenceFilters(
                            databaseConfig,
                            sequenceFilterFields,
                            nucleotideSequenceFieldsEnum(referenceGenome, databaseConfig),
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
                            .properties(getAggregatedResponseProperties(metadataFieldSchemas(databaseConfig))),
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
                            .properties(metadataFieldSchemas(databaseConfig)),
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
                .addSchemas(DETAILS_ORDER_BY_FIELDS_SCHEMA, fieldsArray(databaseConfig.schema.metadata))
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
                    arraySchema(aminoAcidSequenceFieldsEnum(referenceGenome, databaseConfig)),
                )
                .addSchemas(
                    NUCLEOTIDE_SEQUENCES_ORDER_BY_FIELDS_SCHEMA,
                    arraySchema(nucleotideSequenceFieldsEnum(referenceGenome, databaseConfig)),
                )
                .addSchemas(LIMIT_SCHEMA, limitSchema())
                .addSchemas(OFFSET_SCHEMA, offsetSchema())
                .addSchemas(FORMAT_SCHEMA, formatSchema()),
        )
}

private fun getSequenceFiltersWithFormat(
    databaseConfig: DatabaseConfig,
    sequenceFilterFields: SequenceFilterFields,
    orderByFieldsSchema: Schema<Any>,
): Map<SequenceFilterFieldName, Schema<Any>> =
    getSequenceFilters(databaseConfig, sequenceFilterFields, orderByFieldsSchema) +
        Pair(FORMAT_PROPERTY, formatSchema())

private fun getSequenceFilters(
    databaseConfig: DatabaseConfig,
    sequenceFilterFields: SequenceFilterFields,
    orderByFieldsSchema: Schema<Any>,
): Map<SequenceFilterFieldName, Schema<Any>> =
    computePrimitiveFieldFilters(databaseConfig, sequenceFilterFields) +
        Pair(NUCLEOTIDE_MUTATIONS_PROPERTY, nucleotideMutations()) +
        Pair(AMINO_ACID_MUTATIONS_PROPERTY, aminoAcidMutations()) +
        Pair(NUCLEOTIDE_INSERTIONS_PROPERTY, nucleotideInsertions()) +
        Pair(AMINO_ACID_INSERTIONS_PROPERTY, aminoAcidInsertions()) +
        Pair(ORDER_BY_PROPERTY, orderByPostSchema(orderByFieldsSchema)) +
        Pair(LIMIT_PROPERTY, limitSchema()) +
        Pair(OFFSET_PROPERTY, offsetSchema())

private fun computePrimitiveFieldFilters(
    databaseConfig: DatabaseConfig,
    sequenceFilterFields: SequenceFilterFields,
): Map<SequenceFilterFieldName, Schema<Any>> =
    when (databaseConfig.schema.opennessLevel) {
        OpennessLevel.PROTECTED -> primitiveSequenceFilterFieldSchemas(sequenceFilterFields) +
            ("accessKey" to accessKeySchema())

        else -> primitiveSequenceFilterFieldSchemas(sequenceFilterFields)
    }

private fun lapisResponseSchema(dataSchema: Schema<Any>) =
    Schema<Any>().type("object").properties(
        mapOf(
            "data" to Schema<Any>().type("array").items(dataSchema),
            "info" to infoResponseSchema(),
        ),
    ).required(listOf("data", "info"))

private fun infoResponseSchema() =
    Schema<Any>().type("object")
        .description("Information about LAPIS.")
        .properties(
            mapOf(
                "dataVersion" to Schema<String>().type("string")
                    .description(
                        "$LAPIS_DATA_VERSION_DESCRIPTION " +
                            "Same as the value of the header \"$LAPIS_DATA_VERSION_HEADER\".",
                    )
                    .example("1702305399"),
            ),
        ).required(listOf("dataVersion"))

private fun metadataFieldSchemas(databaseConfig: DatabaseConfig) =
    databaseConfig.schema.metadata.associate { it.name to Schema<String>().type(mapToOpenApiType(it.type)) }

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
        .map { (fieldName, fieldType) -> fieldName to Schema<String>().type(fieldType.openApiType) }
        .toMap()

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
        COUNT_PROPERTY to Schema<String>().type("integer").description("The number of sequences matching the filters."),
    )

private fun accessKeySchema() =
    Schema<String>()
        .type("string")
        .description(
            "An access key that grants access to the protected data that this instance serves. " +
                "There are two types or access keys: One only grants access to aggregated data, " +
                "the other also grants access to detailed data.",
        )

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
        .items(
            Schema<String>()
                .type("string")
                .example("sequence1:A123T")
                .description(
                    """
                    |A nucleotide mutation in the format "\<sequenceName\>?:\<fromSymbol\>?\<position\>\<toSymbol\>?".  
                    |If the sequenceName is not provided, LAPIS will use the default sequence name. 
                    |The fromSymbol is optional. 
                    |If the toSymbol is not provided, the statement means "has any mutation at the given position". 
                    """.trimMargin(),
                ),
        )

private fun aminoAcidMutations() =
    Schema<List<AminoAcidMutation>>()
        .type("array")
        .items(
            Schema<String>()
                .type("string")
                .example("S:123T")
                .description(
                    """
                    |A amino acid mutation in the format "\<gene\>:\<position\>\<toSymbol\>?".  
                    |If the toSymbol is not provided, the statement means "has any mutation at the given position". 
                    """.trimMargin(),
                ),
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

private fun orderByGetSchema(orderByFieldsSchema: Schema<Any>) =
    Schema<List<String>>()
        .type("array")
        .items(orderByFieldsSchema)
        .description("The fields by which the result is ordered in ascending order.")

private fun orderByPostSchema(orderByFieldsSchema: Schema<Any>) =
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
        ._enum(listOf("csv", "tsv", "json"))
        ._default("json")

private fun fieldsArray(
    databaseConfig: List<DatabaseMetadata>,
    additionalFields: List<String> = emptyList(),
) = arraySchema(fieldsEnum(databaseConfig, additionalFields))

private fun aggregatedOrderByFieldsEnum(databaseConfig: DatabaseConfig) =
    fieldsEnum(databaseConfig.schema.metadata, listOf("count"))

private fun mutationsOrderByFieldsEnum() = fieldsEnum(emptyList(), listOf("mutation", "count", "proportion"))

private fun insertionsOrderByFieldsEnum() = fieldsEnum(emptyList(), listOf("insertion", "count"))

private fun aminoAcidSequenceFieldsEnum(
    referenceGenome: ReferenceGenome,
    databaseConfig: DatabaseConfig,
) = fieldsEnum(emptyList(), referenceGenome.genes.map { it.name } + databaseConfig.schema.primaryKey)

private fun nucleotideSequenceFieldsEnum(
    referenceGenome: ReferenceGenome,
    databaseConfig: DatabaseConfig,
) = fieldsEnum(emptyList(), referenceGenome.nucleotideSequences.map { it.name } + databaseConfig.schema.primaryKey)

private fun fieldsEnum(
    databaseConfig: List<DatabaseMetadata>,
    additionalFields: List<String> = emptyList(),
) = Schema<String>()
    .type("string")
    ._enum(databaseConfig.map { it.name } + additionalFields)

private fun arraySchema(schema: Schema<Any>) =
    Schema<Any>()
        .type("array")
        .items(schema)
