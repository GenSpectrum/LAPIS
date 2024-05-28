package org.genspectrum.lapis.util

import org.springframework.stereotype.Component

@Component
object TimeFactory {
    fun now() = System.currentTimeMillis()
}
