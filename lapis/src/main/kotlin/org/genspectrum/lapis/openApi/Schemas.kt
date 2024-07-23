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
import org.genspectrum.lapis.controller.AGGREGATED_GROUP_BY_FIELDS_DESCRIPTION
import org.genspectrum.lapis.controller.AGGREGATED_ORDER_BY_FIELDS_DESCRIPTION
import org.genspectrum.lapis.controller.ALIGNED_AMINO_ACID_SEQUENCE_ENDPOINT_DESCRIPTION
import org.genspectrum.lapis.controller.ALIGNED_MULTI_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION
import org.genspectrum.lapis.controller.ALIGNED_SINGLE_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION
import org.genspectrum.lapis.controller.AMINO_ACID_INSERTIONS_ENDPOINT_DESCRIPTION
import org.genspectrum.lapis.controller.AMINO_ACID_MUTATIONS_ENDPOINT_DESCRIPTION
import org.genspectrum.lapis.controller.DETAILS_ENDPOINT_DESCRIPTION
import org.genspectrum.lapis.controller.DETAILS_FIELDS_DESCRIPTION
import org.genspectrum.lapis.controller.DETAILS_ORDER_BY_FIELDS_DESCRIPTION
import org.genspectrum.lapis.controller.FORMAT_DESCRIPTION
import org.genspectrum.lapis.controller.LIMIT_DESCRIPTION
import org.genspectrum.lapis.controller.LapisHeaders.LAPIS_DATA_VERSION
import org.genspectrum.lapis.controller.LapisHeaders.REQUEST_ID
import org.genspectrum.lapis.controller.NUCLEOTIDE_INSERTIONS_ENDPOINT_DESCRIPTION
import org.genspectrum.lapis.controller.NUCLEOTIDE_MUTATION_ENDPOINT_DESCRIPTION
import org.genspectrum.lapis.controller.OFFSET_DESCRIPTION
import org.genspectrum.lapis.controller.UNALIGNED_MULTI_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION
import org.genspectrum.lapis.controller.UNALIGNED_SINGLE_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION
import org.springframework.core.annotation.AliasFor
import org.springframework.http.HttpHeaders.ACCEPT_ENCODING
import org.springframework.http.HttpHeaders.CONTENT_DISPOSITION

const val PRIMITIVE_FIELD_FILTERS_SCHEMA = "SequenceFilters"
const val REQUEST_SCHEMA_WITH_MIN_PROPORTION = "SequenceFiltersWithMinProportion"
const val AGGREGATED_REQUEST_SCHEMA = "AggregatedPostRequest"
const val DETAILS_REQUEST_SCHEMA = "DetailsPostRequest"
const val INSERTIONS_REQUEST_SCHEMA = "InsertionsRequest"
const val ALIGNED_AMINO_ACID_SEQUENCE_REQUEST_SCHEMA = "AminoAcidSequenceRequest"
const val NUCLEOTIDE_SEQUENCE_REQUEST_SCHEMA = "NucleotideSequenceRequest"

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

const val AGGREGATED_ORDER_BY_FIELDS_SCHEMA = "AggregatedOrderByFields"
const val DETAILS_ORDER_BY_FIELDS_SCHEMA = "DetailsOrderByFields"
const val MUTATIONS_ORDER_BY_FIELDS_SCHEMA = "MutationsOrderByFields"
const val INSERTIONS_ORDER_BY_FIELDS_SCHEMA = "InsertionsOrderByFields"
const val AMINO_ACID_SEQUENCES_ORDER_BY_FIELDS_SCHEMA = "AminoAcidSequencesOrderByFields"
const val NUCLEOTIDE_SEQUENCES_ORDER_BY_FIELDS_SCHEMA = "NucleotideSequencesOrderByFields"
const val LIMIT_SCHEMA = "Limit"
const val OFFSET_SCHEMA = "Offset"
const val FORMAT_SCHEMA = "DataFormat"
const val FIELDS_TO_AGGREGATE_BY_SCHEMA = "FieldsToAggregateBy"
const val DETAILS_FIELDS_SCHEMA = "DetailsFields"
const val GENE_SCHEMA = "Gene"
const val SEGMENT_SCHEMA = "Segment"

const val LAPIS_INFO_DESCRIPTION = "Information about LAPIS."
const val LAPIS_DATA_VERSION_EXAMPLE = "1702305399"
const val LAPIS_DATA_VERSION_DESCRIPTION = "The data version of data in SILO."
const val LAPIS_DATA_VERSION_HEADER_DESCRIPTION =
    "$LAPIS_DATA_VERSION_DESCRIPTION " +
        "Same as the value returned in the info object in the response body."
const val LAPIS_DATA_VERSION_RESPONSE_DESCRIPTION =
    "$LAPIS_DATA_VERSION_DESCRIPTION " +
        "Same as the value returned in the info object in the header '$LAPIS_DATA_VERSION'."

const val REQUEST_ID_HEADER_DESCRIPTION =
    """
A UUID that uniquely identifies the request for tracing purposes.
If none if provided in the request, LAPIS will generate one.
"""

const val DOWNLOAD_AS_FILE_DESCRIPTION =
    """
Set to true to make your browser trigger a download instead of showing the response content by setting the
'$CONTENT_DISPOSITION' header to 'attachment'.
"""

const val DOWNLOAD_FILE_BASENAME_DESCRIPTION =
    """
Specify the download file basename, for example, specifying 'myFile' will result in a file named 'myFile.json' 
(when no compression is selected). 
This parameter only takes effect when 'downloadAsFile' is set to true.
"""

const val COMPRESSION_DESCRIPTION =
    """
Optionally set this to return the response compressed in the specified format.
Alternatively, you can set the '$ACCEPT_ENCODING' header to the respective value.
"""

const val ACCESS_KEY_DESCRIPTION =
    """
An access key that grants access to the protected data that this instance serves.
There are two types or access keys: One only grants access to aggregated data,
the other also grants access to detailed data.
"""

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Operation
@ApiResponse(
    responseCode = "200",
    description = "OK",
    headers = [
        Header(
            name = LAPIS_DATA_VERSION,
            description = LAPIS_DATA_VERSION_HEADER_DESCRIPTION,
            schema = Schema(type = "string"),
        ),
        Header(
            name = REQUEST_ID,
            description = REQUEST_ID_HEADER_DESCRIPTION,
            schema = Schema(type = "string"),
        ),
    ],
)
annotation class LapisResponseAnnotation(
    @get:AliasFor(annotation = Operation::class, attribute = "description")
    val description: String,
    @get:AliasFor(annotation = ApiResponse::class, attribute = "content")
    val content: Array<Content> = [],
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@LapisResponseAnnotation(
    description = AGGREGATED_ENDPOINT_DESCRIPTION,
    content = [
        Content(
            schema = Schema(
                ref = "#/components/schemas/$AGGREGATED_RESPONSE_SCHEMA",
            ),
        ),
    ],
)
annotation class LapisAggregatedResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@LapisResponseAnnotation(
    description = NUCLEOTIDE_MUTATION_ENDPOINT_DESCRIPTION,
    content = [
        Content(
            schema = Schema(
                ref = "#/components/schemas/$NUCLEOTIDE_MUTATIONS_RESPONSE_SCHEMA",
            ),
        ),
    ],
)
annotation class LapisNucleotideMutationsResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@LapisResponseAnnotation(
    description = AMINO_ACID_MUTATIONS_ENDPOINT_DESCRIPTION,
    content = [
        Content(
            schema = Schema(
                ref = "#/components/schemas/$AMINO_ACID_MUTATIONS_RESPONSE_SCHEMA",
            ),
        ),
    ],
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
    content = [
        Content(
            schema = Schema(
                ref = "#/components/schemas/$NUCLEOTIDE_INSERTIONS_RESPONSE_SCHEMA",
            ),
        ),
    ],
)
annotation class LapisNucleotideInsertionsResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@LapisResponseAnnotation(
    description = AMINO_ACID_INSERTIONS_ENDPOINT_DESCRIPTION,
    content = [
        Content(
            schema = Schema(
                ref = "#/components/schemas/$AMINO_ACID_INSERTIONS_RESPONSE_SCHEMA",
            ),
        ),
    ],
)
annotation class LapisAminoAcidInsertionsResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@LapisResponseAnnotation(
    description = ALIGNED_AMINO_ACID_SEQUENCE_ENDPOINT_DESCRIPTION,
    content = [Content(schema = Schema(type = "string"))],
)
annotation class LapisAlignedAminoAcidSequenceResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@LapisResponseAnnotation(
    description = ALIGNED_SINGLE_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION,
    content = [Content(schema = Schema(type = "string"))],
)
annotation class LapisAlignedSingleSegmentedNucleotideSequenceResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@LapisResponseAnnotation(
    description = UNALIGNED_SINGLE_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION,
    content = [Content(schema = Schema(type = "string"))],
)
annotation class LapisUnalignedSingleSegmentedNucleotideSequenceResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@LapisResponseAnnotation(
    description = ALIGNED_MULTI_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION,
    content = [Content(schema = Schema(type = "string"))],
)
annotation class LapisAlignedMultiSegmentedNucleotideSequenceResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@LapisResponseAnnotation(
    description = UNALIGNED_MULTI_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION,
    content = [Content(schema = Schema(type = "string"))],
)
annotation class LapisUnalignedMultiSegmentedNucleotideSequenceResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    responses = [
        ApiResponse(
            responseCode = "200",
            content = [Content(schema = Schema(type = "string"))],
        ),
    ],
)
annotation class StringResponseOperation(
    @get:AliasFor(annotation = Operation::class, attribute = "description")
    val description: String = "",
    @get:AliasFor(annotation = Operation::class, attribute = "operationId")
    val operationId: String,
)

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Parameter(
    description =
        "Valid filters for sequence data. This may be empty. Only provide the fields that should be filtered by.",
    schema = Schema(ref = "#/components/schemas/$PRIMITIVE_FIELD_FILTERS_SCHEMA"),
    explode = Explode.TRUE,
    style = ParameterStyle.FORM,
)
annotation class PrimitiveFieldFilters

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Parameter(
    schema = Schema(ref = "#/components/schemas/$AGGREGATED_ORDER_BY_FIELDS_SCHEMA"),
    description = AGGREGATED_ORDER_BY_FIELDS_DESCRIPTION,
)
annotation class AggregatedOrderByFields

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Parameter(
    schema = Schema(ref = "#/components/schemas/$DETAILS_ORDER_BY_FIELDS_SCHEMA"),
    description = DETAILS_ORDER_BY_FIELDS_DESCRIPTION,
)
annotation class DetailsOrderByFields

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Parameter(
    schema = Schema(ref = "#/components/schemas/$MUTATIONS_ORDER_BY_FIELDS_SCHEMA"),
    description = "The fields of the response to order by.",
)
annotation class MutationsOrderByFields

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Parameter(
    schema = Schema(ref = "#/components/schemas/$INSERTIONS_ORDER_BY_FIELDS_SCHEMA"),
    description = "The fields of the response to order by.",
)
annotation class InsertionsOrderByFields

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Parameter(
    schema = Schema(ref = "#/components/schemas/$AMINO_ACID_SEQUENCES_ORDER_BY_FIELDS_SCHEMA"),
    description = "The parts of the fasta header to order by.",
)
annotation class AminoAcidSequencesOrderByFields

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Parameter(
    schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_SEQUENCES_ORDER_BY_FIELDS_SCHEMA"),
    description = "The parts of the fasta header to order by.",
)
annotation class NucleotideSequencesOrderByFields

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

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Parameter(
    schema = Schema(ref = "#/components/schemas/$FIELDS_TO_AGGREGATE_BY_SCHEMA"),
    description = AGGREGATED_GROUP_BY_FIELDS_DESCRIPTION,
)
annotation class FieldsToAggregateBy

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Parameter(
    schema = Schema(ref = "#/components/schemas/$DETAILS_FIELDS_SCHEMA"),
    description = DETAILS_FIELDS_DESCRIPTION,
)
annotation class DetailsFields

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Parameter(
    schema = Schema(ref = "#/components/schemas/$GENE_SCHEMA"),
)
annotation class Gene

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Parameter(
    schema = Schema(ref = "#/components/schemas/$SEGMENT_SCHEMA"),
)
annotation class Segment
