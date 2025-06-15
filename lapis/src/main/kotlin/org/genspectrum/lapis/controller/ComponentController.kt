package org.genspectrum.lapis.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import org.genspectrum.lapis.controller.LapisHeaders.LAPIS_DATA_VERSION
import org.genspectrum.lapis.model.mutationsOverTime.MutationsOverTime
import org.genspectrum.lapis.openApi.MUTATIONS_OVER_TIME_REQUEST_SCHEMA
import org.genspectrum.lapis.openApi.MutationsOverTimeResponse
import org.genspectrum.lapis.request.AminoAcidMutationsOverTimeRequest
import org.genspectrum.lapis.request.NucleotideMutationsOverTimeRequest
import org.genspectrum.lapis.response.LapisInfoFactory
import org.genspectrum.lapis.silo.DataVersion
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/component")
class ComponentController(
    val mutationsOverTime: MutationsOverTime,
    val lapisInfoFactory: LapisInfoFactory,
    val dataVersion: DataVersion,
) {
    @PostMapping(
        "/nucleotideMutationsOverTime",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    @MutationsOverTimeResponse
    @Operation(
        operationId = "postNucleotideMutationsOverTime",
    )
    fun postMutationsOverTime(
        @Parameter(schema = Schema(ref = "#/components/schemas/$MUTATIONS_OVER_TIME_REQUEST_SCHEMA"))
        @RequestBody
        request: NucleotideMutationsOverTimeRequest,
    ): ResponseEntity<Map<String, Any>?> {
        val data = mutationsOverTime.evaluateNucleotideMutations(
            request.includeMutations,
            request.dateRanges,
            request,
            request.dateField,
        )

        val body = mapOf(
            "data" to data,
            "info" to lapisInfoFactory.create(),
        )

        return ResponseEntity
            .ok()
            .header(LAPIS_DATA_VERSION, dataVersion.dataVersion)
            .body(body)
    }

    @PostMapping(
        "/aminoAcidMutationsOverTime",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    @MutationsOverTimeResponse
    @Operation(
        operationId = "postAminoAcidMutationsOverTime",
    )
    fun postAminoAcidMutationsOverTime(
        @Parameter(schema = Schema(ref = "#/components/schemas/$MUTATIONS_OVER_TIME_REQUEST_SCHEMA"))
        @RequestBody
        request: AminoAcidMutationsOverTimeRequest,
    ): ResponseEntity<Map<String, Any>?> {
        val data = mutationsOverTime.evaluateAminoAcidMutations(
            request.includeMutations,
            request.dateRanges,
            request,
            request.dateField,
        )

        val body = mapOf(
            "data" to data,
            "info" to lapisInfoFactory.create(),
        )

        return ResponseEntity
            .ok()
            .header(LAPIS_DATA_VERSION, dataVersion.dataVersion)
            .body(body)
    }
}
