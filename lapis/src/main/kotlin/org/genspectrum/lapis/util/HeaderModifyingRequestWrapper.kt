package org.genspectrum.lapis.util

import jakarta.servlet.http.HttpServletRequestWrapper
import mu.KotlinLogging
import java.util.Collections
import java.util.Enumeration

private val log = KotlinLogging.logger {}

class HeaderModifyingRequestWrapper(
    private val reReadableRequest: CachedBodyHttpServletRequest,
    private val headerName: String,
    private val computeHeaderValueFromRequest: (CachedBodyHttpServletRequest) -> String?,
) : HttpServletRequestWrapper(reReadableRequest) {
    override fun getHeader(name: String): String? {
        if (name.equals(headerName, ignoreCase = true)) {
            when (val overwrittenValue = computeHeaderValueFromRequest(reReadableRequest)) {
                null -> {}
                else -> return overwriteWith(overwrittenValue)
            }
        }

        return super.getHeader(name)
    }

    override fun getHeaders(name: String): Enumeration<String>? {
        if (name.equals(headerName, ignoreCase = true)) {
            when (val overwrittenValue = computeHeaderValueFromRequest(reReadableRequest)) {
                null -> {}
                else -> return Collections.enumeration(listOf(overwriteWith(overwrittenValue)))
            }
        }

        return super.getHeaders(name)
    }

    override fun getHeaderNames(): Enumeration<String> =
        when (computeHeaderValueFromRequest(reReadableRequest)) {
            null -> super.getHeaderNames()
            else -> Collections.enumeration(super.getHeaderNames().toList().toSet() + headerName)
        }

    private fun overwriteWith(value: String): String {
        log.debug { "Overwriting $headerName header to $value" }
        return value
    }
}
