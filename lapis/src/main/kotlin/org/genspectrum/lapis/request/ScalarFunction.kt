package org.genspectrum.lapis.request

import org.genspectrum.lapis.config.MetadataType

/**
 * An enum of scalar functions supported by SILO.
 * New functions need to be whitelisted here explicitly.
 */
enum class ScalarFunction(
    val saneQlMethodName: String,
    val validForTypes: Set<MetadataType>,
) {
    ISO_WEEK("isoWeek", setOf(MetadataType.DATE)),
}
