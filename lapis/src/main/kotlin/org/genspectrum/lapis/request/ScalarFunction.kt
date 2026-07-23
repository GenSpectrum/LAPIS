package org.genspectrum.lapis.request

import org.genspectrum.lapis.config.MetadataType

enum class ScalarFunction(
    val saneQlMethodName: String,
    val validForTypes: Set<MetadataType>,
) {
    ISO_WEEK("isoWeek", setOf(MetadataType.DATE)),
}
