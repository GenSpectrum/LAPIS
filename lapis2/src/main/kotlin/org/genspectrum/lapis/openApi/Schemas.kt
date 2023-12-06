package org.genspectrum.lapis.openApi

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.Explode
import io.swagger.v3.oas.annotations.enums.ParameterStyle
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.genspectrum.lapis.controller.AGGREGATED_ENDPOINT_DESCRIPTION
import org.genspectrum.lapis.controller.ALIGNED_MULTI_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION
import org.genspectrum.lapis.controller.ALIGNED_SINGLE_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION
import org.genspectrum.lapis.controller.AMINO_ACID_INSERTIONS_ENDPOINT_DESCRIPTION
import org.genspectrum.lapis.controller.AMINO_ACID_MUTATIONS_ENDPOINT_DESCRIPTION
import org.genspectrum.lapis.controller.AMINO_ACID_SEQUENCE_ENDPOINT_DESCRIPTION
import org.genspectrum.lapis.controller.DETAILS_ENDPOINT_DESCRIPTION
import org.genspectrum.lapis.controller.FORMAT_DESCRIPTION
import org.genspectrum.lapis.controller.LIMIT_DESCRIPTION
import org.genspectrum.lapis.controller.NUCLEOTIDE_INSERTIONS_ENDPOINT_DESCRIPTION
import org.genspectrum.lapis.controller.NUCLEOTIDE_MUTATION_ENDPOINT_DESCRIPTION
import org.genspectrum.lapis.controller.OFFSET_DESCRIPTION
import org.genspectrum.lapis.request.LAPIS_DATA_VERSION_HEADER
import org.springframework.core.annotation.AliasFor

const val SEQUENCE_FILTERS_SCHEMA = "SequenceFilters"
const val REQUEST_SCHEMA_WITH_MIN_PROPORTION = "SequenceFiltersWithMinProportion"
const val AGGREGATED_REQUEST_SCHEMA = "AggregatedPostRequest"
const val DETAILS_REQUEST_SCHEMA = "DetailsPostRequest"
const val INSERTIONS_REQUEST_SCHEMA = "InsertionsRequest"
const val SEQUENCE_REQUEST_SCHEMA = "SequenceRequest"

const val AGGREGATED_RESPONSE_SCHEMA = "AggregatedResponse"
const val DETAILS_RESPONSE_SCHEMA = "DetailsResponse"
const val NUCLEOTIDE_MUTATIONS_RESPONSE_SCHEMA = "NucleotideMutationsResponse"
const val AMINO_ACID_MUTATIONS_RESPONSE_SCHEMA = "AminoAcidMutationsResponse"
const val NUCLEOTIDE_INSERTIONS_RESPONSE_SCHEMA = "NucleotideInsertionsResponse"
const val AMINO_ACID_INSERTIONS_RESPONSE_SCHEMA = "AminoAcidInsertionsResponse"

const val NUCLEOTIDE_MUTATIONS_SCHEMA = "NucleotideMutations"
const val AMINO_ACID_MUTATIONS_SCHEMA = "AminoAcidMutations"
const val NUCLEOTIDE_INSERTIONS_SCHEMA = "NucleotideInsertions"
const val AMINO_ACID_INSERTIONS_SCHEMA = "AminoAcidInsertions"

const val ORDER_BY_FIELDS_SCHEMA = "OrderByFields"
const val LIMIT_SCHEMA = "Limit"
const val OFFSET_SCHEMA = "Offset"
const val FORMAT_SCHEMA = "DataFormat"


@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Operation
@ApiResponse(
    responseCode = "200",
    description = "OK",
    headers = [
        Header(
            name = LAPIS_DATA_VERSION_HEADER,
            description = "A timestamp of when the data was last updated.",
            schema = Schema(type = "string"),
        ),
    ],
)
annotation class LapisResponseAnnotation(
    @get:AliasFor(annotation = Operation::class, attribute = "description") val description: String,
    @get:AliasFor(annotation = ApiResponse::class, attribute = "content") val content: Array<Content> = [],
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@LapisResponseAnnotation(
    description = AGGREGATED_ENDPOINT_DESCRIPTION,
    content = [Content(schema = Schema(ref = "#/components/schemas/$AGGREGATED_RESPONSE_SCHEMA"))],
)
annotation class LapisAggregatedResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@LapisResponseAnnotation(
    description = NUCLEOTIDE_MUTATION_ENDPOINT_DESCRIPTION,
    content = [Content(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_MUTATIONS_RESPONSE_SCHEMA"))],
)
annotation class LapisNucleotideMutationsResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@LapisResponseAnnotation(
    description = AMINO_ACID_MUTATIONS_ENDPOINT_DESCRIPTION,
    content = [Content(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_MUTATIONS_RESPONSE_SCHEMA"))],
)
annotation class LapisAminoAcidMutationsResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@LapisResponseAnnotation(
    description = DETAILS_ENDPOINT_DESCRIPTION,
    content = [Content(schema = Schema(ref = "#/components/schemas/$DETAILS_RESPONSE_SCHEMA"))],
)
annotation class LapisDetailsResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@LapisResponseAnnotation(
    description = NUCLEOTIDE_INSERTIONS_ENDPOINT_DESCRIPTION,
    content = [Content(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_INSERTIONS_RESPONSE_SCHEMA"))],
)
annotation class LapisNucleotideInsertionsResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@LapisResponseAnnotation(
    description = AMINO_ACID_INSERTIONS_ENDPOINT_DESCRIPTION,
    content = [Content(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_INSERTIONS_RESPONSE_SCHEMA"))],
)
annotation class LapisAminoAcidInsertionsResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@LapisResponseAnnotation(
    description = AMINO_ACID_SEQUENCE_ENDPOINT_DESCRIPTION,
)
annotation class LapisAminoAcidSequenceResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@LapisResponseAnnotation(
    description = ALIGNED_SINGLE_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION,
)
annotation class LapisAlignedSingleSegmentedNucleotideSequenceResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@LapisResponseAnnotation(
    description = ALIGNED_MULTI_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION,
)
annotation class LapisAlignedMultiSegmentedNucleotideSequenceResponse

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Parameter(
    description =
    "Valid filters for sequence data. This may be empty. Only provide the fields that should be filtered by.",
    schema = Schema(ref = "#/components/schemas/$SEQUENCE_FILTERS_SCHEMA"),
    explode = Explode.TRUE,
    style = ParameterStyle.FORM,
)
annotation class SequenceFilters

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Parameter(
    schema = Schema(ref = "#/components/schemas/$ORDER_BY_FIELDS_SCHEMA"),
)
annotation class OrderByFields(
    @get:AliasFor(
        annotation = Parameter::class,
        attribute = "description",
    ) val value: String = "The fields of the response to order by.",
)

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Parameter(
    schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_MUTATIONS_SCHEMA"),
    explode = Explode.TRUE,
)
annotation class NucleotideMutations

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_MUTATIONS_SCHEMA"))
annotation class AminoAcidMutations

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_INSERTIONS_SCHEMA"))
annotation class NucleotideInsertions

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_INSERTIONS_SCHEMA"))
annotation class AminoAcidInsertions

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Parameter(
    schema = Schema(ref = "#/components/schemas/$LIMIT_SCHEMA"),
    description = LIMIT_DESCRIPTION,
)
annotation class Limit

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Parameter(
    schema = Schema(ref = "#/components/schemas/$OFFSET_SCHEMA"),
    description = OFFSET_DESCRIPTION,
)
annotation class Offset

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Parameter(
    schema = Schema(ref = "#/components/schemas/$FORMAT_SCHEMA"),
    description = FORMAT_DESCRIPTION,
)
annotation class DataFormat
