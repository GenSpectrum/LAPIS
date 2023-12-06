package org.genspectrum.lapis.openApi

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.MetadataType
import org.genspectrum.lapis.config.OpennessLevel
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
import org.genspectrum.lapis.request.NucleotideInsertion
import org.genspectrum.lapis.request.NucleotideMutation
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.response.COUNT_PROPERTY

fun buildOpenApiSchema(sequenceFilterFields: SequenceFilterFields, databaseConfig: DatabaseConfig): OpenAPI {
    val requestProperties = when (databaseConfig.schema.opennessLevel) {
        OpennessLevel.PROTECTED -> primitiveSequenceFilterFieldSchemas(sequenceFilterFields) +
            ("accessKey" to accessKeySchema())

        else -> primitiveSequenceFilterFieldSchemas(sequenceFilterFields)
    }

    val sequenceFilters = requestProperties +
        Pair(NUCLEOTIDE_MUTATIONS_PROPERTY, nucleotideMutations()) +
        Pair(AMINO_ACID_MUTATIONS_PROPERTY, aminoAcidMutations()) +
        Pair(NUCLEOTIDE_INSERTIONS_PROPERTY, nucleotideInsertions()) +
        Pair(AMINO_ACID_INSERTIONS_PROPERTY, aminoAcidInsertions()) +
        Pair(ORDER_BY_PROPERTY, orderByPostSchema()) +
        Pair(LIMIT_PROPERTY, limitSchema()) +
        Pair(OFFSET_PROPERTY, offsetSchema())

    val sequenceFiltersWithFormat = sequenceFilters + Pair(FORMAT_PROPERTY, formatSchema())

    return OpenAPI()
        .components(
            Components()
                .addSchemas(
                    SEQUENCE_FILTERS_SCHEMA,
                    Schema<String>()
                        .type("object")
                        .description("valid filters for sequence data")
                        .properties(requestProperties),
                )
                .addSchemas(
                    REQUEST_SCHEMA_WITH_MIN_PROPORTION,
                    Schema<String>()
                        .type("object")
                        .description("valid filters for sequence data")
                        .properties(
                            sequenceFiltersWithFormat + Pair(MIN_PROPORTION_PROPERTY, Schema<String>().type("number")),
                        ),
                )
                .addSchemas(
                    AGGREGATED_REQUEST_SCHEMA,
                    requestSchemaWithFields(sequenceFiltersWithFormat, AGGREGATED_GROUP_BY_FIELDS_DESCRIPTION),
                )
                .addSchemas(
                    DETAILS_REQUEST_SCHEMA,
                    requestSchemaWithFields(sequenceFiltersWithFormat, DETAILS_FIELDS_DESCRIPTION),
                )
                .addSchemas(
                    INSERTIONS_REQUEST_SCHEMA,
                    requestSchemaForCommonSequenceFilters(sequenceFiltersWithFormat),
                )
                .addSchemas(
                    SEQUENCE_REQUEST_SCHEMA,
                    requestSchemaForCommonSequenceFilters(sequenceFilters),
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
                .addSchemas(AMINO_ACID_MUTATIONS_SCHEMA, aminoAcidMutations())
                .addSchemas(NUCLEOTIDE_INSERTIONS_SCHEMA, nucleotideInsertions())
                .addSchemas(AMINO_ACID_INSERTIONS_SCHEMA, aminoAcidInsertions())
                .addSchemas(ORDER_BY_FIELDS_SCHEMA, orderByGetSchema())
                .addSchemas(LIMIT_SCHEMA, limitSchema())
                .addSchemas(OFFSET_SCHEMA, offsetSchema())
                .addSchemas(FORMAT_SCHEMA, formatSchema()),
        )
}

private fun lapisResponseSchema(dataSchema: Schema<Any>) =
    Schema<Any>().type("object").properties(
        mapOf(
            "data" to Schema<Any>().type("array").items(dataSchema),
        ),
    ).required(listOf("data"))

private fun metadataFieldSchemas(databaseConfig: DatabaseConfig) =
    databaseConfig.schema.metadata.associate { it.name to Schema<String>().type(mapToOpenApiType(it.type)) }

private fun proportionSchema() =
    mapOf(
        "mutation" to Schema<String>().type("string").description("The mutation that was found."),
        "proportion" to Schema<String>().type("number").description("The proportion of sequences having the mutation."),
        "count" to Schema<String>().type("number").description("The number of sequences matching having the mutation."),
    )

private fun mapToOpenApiType(type: MetadataType): String = when (type) {
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
): Schema<*> =
    Schema<String>()
        .type("object")
        .description("valid filters for sequence data")
        .properties(requestProperties + Pair(FIELDS_PROPERTY, fieldsSchema().description(fieldsDescription)))

private fun getAggregatedResponseProperties(filterProperties: Map<SequenceFilterFieldName, Schema<Any>>) =
    filterProperties.mapValues { (_, schema) ->
        schema.description(
            "This field is present if and only if it was specified in \"fields\" in the request. " +
                "The response is stratified by this field.",
        )
    } + mapOf(
        COUNT_PROPERTY to Schema<String>().type("number").description("The number of sequences matching the filters."),
    )

private fun accessKeySchema() = Schema<String>()
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
        "count" to Schema<String>().type("number").description("The number of sequences matching having the mutation."),
    )

private fun aminoAcidMutationProportionSchema() =
    mapOf(
        "mutation" to Schema<String>().type("string").example("ORF1a:123").description(
            "A amino acid mutation that was found in the format \"\\<gene\\>:\\<position\\>",
        ),
        "proportion" to Schema<String>().type("number").description("The proportion of sequences having the mutation."),
        "count" to Schema<String>().type("number").description("The number of sequences matching having the mutation."),
    )

private fun nucleotideInsertionSchema() =
    mapOf(
        "insertion" to Schema<String>().type("string")
            .example("ins_segment:123:AAT")
            .description("The insertion that was found."),
        "count" to Schema<String>().type("number")
            .description("The number of sequences matching having the insertion."),
    )

private fun aminoAcidInsertionSchema() =
    mapOf(
        "insertion" to Schema<String>().type("string")
            .example("ins_gene:123:AAT")
            .description("The insertion that was found."),
        "count" to Schema<String>().type("number")
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

private fun orderByGetSchema() = Schema<List<String>>()
    .type("array")
    .items(orderByFieldStringSchema())
    .description("The fields by which the result is ordered in ascending order.")

private fun orderByPostSchema() = Schema<List<String>>()
    .type("array")
    .items(
        Schema<String>().anyOf(
            listOf(
                orderByFieldStringSchema(),
                Schema<OrderByField>()
                    .type("object")
                    .description("The fields by which the result is ordered with ascending or descending order.")
                    .required(listOf("field"))
                    .properties(
                        mapOf(
                            "field" to orderByFieldStringSchema(),
                            "type" to Schema<String>()
                                .type("string")
                                ._enum(listOf("ascending", "descending"))
                                ._default("ascending"),
                        ),
                    ),
            ),
        ),
    )

private fun orderByFieldStringSchema() = Schema<String>()
    .type("string")
    .example("country")
    .description("The field by which the result is ordered.")

private fun limitSchema() = Schema<Int>()
    .type("integer")
    .description(LIMIT_DESCRIPTION)
    .example(100)

private fun offsetSchema() = Schema<Int>()
    .type("integer")
    .description(OFFSET_DESCRIPTION)

// This is a function so that the resulting schema can be reused in multiple places. The setters mutate the instance.
private fun fieldsSchema() = Schema<String>()
    .type("array")
    .items(Schema<String>().type("string"))

private fun formatSchema() = Schema<String>()
    .type("string")
    .description(
        FORMAT_DESCRIPTION,
    )
    ._enum(listOf("csv", "tsv", "json"))
    ._default("json")