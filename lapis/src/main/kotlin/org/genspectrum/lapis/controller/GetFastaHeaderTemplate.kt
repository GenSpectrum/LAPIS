package org.genspectrum.lapis.controller

import org.genspectrum.lapis.controller.middleware.SequencesDataFormat
import org.genspectrum.lapis.request.FASTA_HEADER_TEMPLATE_PROPERTY

fun getFastaHeaderTemplate(
    requestedTemplate: String?,
    defaultTemplate: String,
    sequencesDataFormat: SequencesDataFormat,
): String {
    if (sequencesDataFormat != SequencesDataFormat.FASTA && requestedTemplate != null) {
        throw BadRequestException(
            "$FASTA_HEADER_TEMPLATE_PROPERTY is only applicable for FASTA format, but received: $requestedTemplate",
        )
    }

    return requestedTemplate ?: defaultTemplate
}
