package org.genspectrum.lapis.config

import org.springframework.stereotype.Component

@Component
data class SiloVersion(
    var version: String? = null,
)
