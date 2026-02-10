package org.genspectrum.lapis.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import org.genspectrum.lapis.controller.LapisHeaders.LAPIS_DATA_VERSION
import org.genspectrum.lapis.model.QueryParseModel
import org.genspectrum.lapis.openApi.QUERY_PARSE_REQUEST_SCHEMA
import org.genspectrum.lapis.request.QueryParseRequest
import org.genspectrum.lapis.response.LapisInfoFactory
import org.genspectrum.lapis.response.QueryParseResponse
import org.genspectrum.lapis.silo.DataVersion
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

const val PARSE_ROUTE = "/parse"

@RestController
@RequestMapping("/query")
class QueryController(
    val queryParseModel: QueryParseModel,
    val lapisInfoFactory: LapisInfoFactory,
    val dataVersion: DataVersion,
) {
    @PostMapping(
        PARSE_ROUTE,
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Operation(description = QUERY_PARSE_ENDPOINT_DESCRIPTION)
    fun postParse(
        @Parameter(schema = Schema(ref = "#/components/schemas/$QUERY_PARSE_REQUEST_SCHEMA"))
        @RequestBody
        request: QueryParseRequest,
    ): ResponseEntity<QueryParseResponse> {
        val data = queryParseModel.parseQueries(request.queries)
        return ResponseEntity
            .ok()
            .header(LAPIS_DATA_VERSION, dataVersion.dataVersion)
            .body(QueryParseResponse(data, lapisInfoFactory.create()))
    }
}
