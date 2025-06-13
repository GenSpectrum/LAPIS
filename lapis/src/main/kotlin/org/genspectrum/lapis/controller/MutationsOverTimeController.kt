package org.genspectrum.lapis.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import org.genspectrum.lapis.controller.LapisHeaders.LAPIS_DATA_VERSION
import org.genspectrum.lapis.model.mutationsOverTime.MutationsOverTimeModel
import org.genspectrum.lapis.openApi.MUTATIONS_OVER_TIME_REQUEST_SCHEMA
import org.genspectrum.lapis.request.MutationsOverTimeRequest
import org.genspectrum.lapis.response.LapisInfoFactory
import org.genspectrum.lapis.response.MutationsOverTimeResponse
import org.genspectrum.lapis.silo.DataVersion
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/component")
class MutationsOverTimeController(
    val mutationsOverTimeModel: MutationsOverTimeModel,
    val lapisInfoFactory: LapisInfoFactory,
    val dataVersion: DataVersion,
) {
    @PostMapping(
        "/nucleotideMutationsOverTime",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Operation(description = NUCLEOTIDE_MUTATIONS_OVER_TIME_ENDPOINT_DESCRIPTION)
    fun postMutationsOverTime(
        @Parameter(schema = Schema(ref = "#/components/schemas/$MUTATIONS_OVER_TIME_REQUEST_SCHEMA"))
        @RequestBody
        request: MutationsOverTimeRequest,
    ): ResponseEntity<MutationsOverTimeResponse> {
        val data = mutationsOverTimeModel.evaluate(
            request.includeMutations,
            request.dateRanges,
            request.filters,
            request.dateField,
        )

        return ResponseEntity
            .ok()
            .header(LAPIS_DATA_VERSION, dataVersion.dataVersion)
            .body(MutationsOverTimeResponse(data, lapisInfoFactory.create()))
    }
}
