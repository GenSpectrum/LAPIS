package org.genspectrum.lapis.controller

import org.genspectrum.lapis.controller.middleware.SequencesDataFormat

fun getFastaHeaderTemplate(
    requestedTemplate: String?,
    defaultTemplate: String,
    sequencesDataFormat: SequencesDataFormat,
): String {
    if (sequencesDataFormat != SequencesDataFormat.FASTA) {
        return defaultTemplate
    }

    return requestedTemplate ?: defaultTemplate
}
