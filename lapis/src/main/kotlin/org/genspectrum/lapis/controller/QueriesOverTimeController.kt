package org.genspectrum.lapis.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import org.genspectrum.lapis.controller.LapisHeaders.LAPIS_DATA_VERSION
import org.genspectrum.lapis.model.mutationsOverTime.MutationsOverTimeResult
import org.genspectrum.lapis.model.mutationsOverTime.QueriesOverTimeModel
import org.genspectrum.lapis.openApi.MUTATIONS_OVER_TIME_REQUEST_SCHEMA
import org.genspectrum.lapis.request.AminoAcidMutationsOverTimeRequest
import org.genspectrum.lapis.request.NucleotideMutationsOverTimeRequest
import org.genspectrum.lapis.request.QueriesOverTimeRequest
import org.genspectrum.lapis.response.LapisInfoFactory
import org.genspectrum.lapis.response.MutationsOverTimeResponse
import org.genspectrum.lapis.silo.DataVersion
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

const val NUCLEOTIDE_MUTATIONS_OVER_TIME_ROUTE = "/nucleotideMutationsOverTime"
const val AMINO_ACID_MUTATIONS_OVER_TIME_ROUTE = "/aminoAcidMutationsOverTime"

@RestController
@RequestMapping("/component")
class QueriesOverTimeController(
    val queriesOverTimeModel: QueriesOverTimeModel,
    val lapisInfoFactory: LapisInfoFactory,
    val dataVersion: DataVersion,
) {
    @PostMapping(
        NUCLEOTIDE_MUTATIONS_OVER_TIME_ROUTE,
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Operation(description = MUTATIONS_OVER_TIME_ENDPOINT_DESCRIPTION)
    fun postNucleotideMutationsOverTime(
        @Parameter(schema = Schema(ref = "#/components/schemas/$MUTATIONS_OVER_TIME_REQUEST_SCHEMA"))
        @RequestBody
        request: NucleotideMutationsOverTimeRequest,
    ): ResponseEntity<MutationsOverTimeResponse> {
        val data = queriesOverTimeModel.evaluateNucleotideMutations(
            request.includeMutations,
            request.dateRanges,
            request.filters,
            request.dateField,
        )
        return createMutationsOverTimeResponse(data)
    }

    @PostMapping(
        AMINO_ACID_MUTATIONS_OVER_TIME_ROUTE,
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Operation(description = MUTATIONS_OVER_TIME_ENDPOINT_DESCRIPTION)
    fun postAminoAcidMutationsOverTime(
        @Parameter(schema = Schema(ref = "#/components/schemas/$MUTATIONS_OVER_TIME_REQUEST_SCHEMA"))
        @RequestBody
        request: AminoAcidMutationsOverTimeRequest,
    ): ResponseEntity<MutationsOverTimeResponse> {
        val data = queriesOverTimeModel.evaluateAminoAcidMutations(
            request.includeMutations,
            request.dateRanges,
            request.filters,
            request.dateField,
        )
        return createMutationsOverTimeResponse(data)
    }

    @PostMapping(
        "/queriesOverTime",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Operation(description = MUTATIONS_OVER_TIME_ENDPOINT_DESCRIPTION)
    fun postQueriesOverTime(
//        @Parameter(schema = Schema(ref = "#/components/schemas/$MUTATIONS_OVER_TIME_REQUEST_SCHEMA"))
        @RequestBody
        request: QueriesOverTimeRequest,
    ): ResponseEntity<MutationsOverTimeResponse> {
        val data = queriesOverTimeModel.evaluateQueriesOverTime(
            request.queries,
            request.dateRanges,
            request.filters,
            request.dateField,
        )
        return createMutationsOverTimeResponse(data)
    }

    private fun createMutationsOverTimeResponse(
        resultData: MutationsOverTimeResult,
    ): ResponseEntity<MutationsOverTimeResponse> =
        ResponseEntity
            .ok()
            .header(LAPIS_DATA_VERSION, dataVersion.dataVersion)
            .body(MutationsOverTimeResponse(resultData, lapisInfoFactory.create()))
}
