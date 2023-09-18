package org.genspectrum.lapis.config

import org.springframework.stereotype.Component

const val IS_SEQUENCE_SINGLE_SEGMENTED_FEATURE = "isSingleSegmentedSequence"

@Component
class SingleSegmentedSequenceFeature(private val databaseConfig: DatabaseConfig) {
    fun isEnabled() =
        databaseConfig.schema.features.any { it.name == IS_SEQUENCE_SINGLE_SEGMENTED_FEATURE }
}
