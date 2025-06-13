package org.genspectrum.lapis.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema
import org.genspectrum.lapis.model.mutationsOverTime.MutationsOverTimeResult
import org.genspectrum.lapis.openApi.LAPIS_DATA_VERSION_EXAMPLE
import org.genspectrum.lapis.openApi.LAPIS_DATA_VERSION_RESPONSE_DESCRIPTION
import org.genspectrum.lapis.openApi.LAPIS_INFO_DESCRIPTION
import org.genspectrum.lapis.openApi.REQUEST_ID_HEADER_DESCRIPTION
import org.genspectrum.lapis.openApi.REQUEST_INFO_STRING_DESCRIPTION
import org.genspectrum.lapis.openApi.SILO_VERSION_DESCRIPTION
import org.genspectrum.lapis.openApi.VERSION_DESCRIPTION
import org.springframework.http.ProblemDetail

data class LapisErrorResponse(
    val error: ProblemDetail,
    val info: LapisInfo,
)

private const val REPORT_TO =
    "Please report to https://github.com/GenSpectrum/LAPIS/issues in case you encounter any unexpected issues. " +
        "Please include the request ID and the requestInfo in your report."

@Schema(description = LAPIS_INFO_DESCRIPTION)
data class LapisInfo(
    @Schema(
        description = LAPIS_DATA_VERSION_RESPONSE_DESCRIPTION,
        example = LAPIS_DATA_VERSION_EXAMPLE,
    )
    var dataVersion: String? = null,
    @Schema(
        description = REQUEST_ID_HEADER_DESCRIPTION,
        example = "dfb342ea-3607-4caf-b35e-9aba75d06f81",
    )
    var requestId: String? = null,
    @Schema(
        description = REQUEST_INFO_STRING_DESCRIPTION,
        example = "my_instance on my.server.com at 2024-01-01T12:00:00.0000",
    )
    var requestInfo: String? = null,
    @Schema(example = REPORT_TO)
    val reportTo: String = REPORT_TO,
    @Schema(
        description = VERSION_DESCRIPTION,
        example = "1.2.3",
    )
    val lapisVersion: String? = null,
    @Schema(
        description = SILO_VERSION_DESCRIPTION,
        example = "2.3.4",
    )
    val siloVersion: String? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class MutationResponse(
    val mutation: String?,
    val count: Int?,
    val coverage: Int?,
    val proportion: Double?,
    val sequenceName: ExplicitlyNullable<String>?,
    val mutationFrom: String?,
    val mutationTo: String?,
    val position: Int?,
)

data class InsertionResponse(
    val insertion: String,
    val count: Int,
    val insertedSymbols: String,
    val position: Int,
    val sequenceName: String?,
)

data class MutationsOverTimeResponse(
    val data: MutationsOverTimeResult,
    val info: LapisInfo,
)

/**
 * In Javascript terms, this is equivalent to using `null` instead of `undefined`.
 * ExplicitlyNullable is used to serialize null values in JSON when the surrounding class ignores null values,
 * i.e. treats `null` as `undefined`.
 * In some cases, you still want to be able to explicitly set `null` on a field.
 *
 * Example:
 * ```
 * @JsonInclude(JsonInclude.Include.NON_NULL)
 * data class Example(value: ExplicitlyNullable<String>?)
 * ```
 *
 * - `Example(null)` will be serialized as `{}`.
 * - `Example(ExplicitlyNullable(null))` will be serialized as `{"value":null}`.
 * - `Example(ExplicitlyNullable("my value"))` will be serialized as `{"value":"my value"}`.
 */
data class ExplicitlyNullable<T>(
    @get:JsonValue
    val value: T? = null,
)
