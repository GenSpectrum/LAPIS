package org.genspectrum.lapis.silo

import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

@Component
@RequestScope
data class DataVersion(
    var dataVersion: String? = null,
)
