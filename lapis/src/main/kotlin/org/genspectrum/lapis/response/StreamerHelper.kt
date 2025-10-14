package org.genspectrum.lapis.response

import org.genspectrum.lapis.log
import java.io.IOException

fun <T> streamAndLogDisconnect(
    dataTypeName: String,
    callback: () -> T,
) = try {
    callback()
} catch (e: IOException) {
    log.info { "Client likely disconnected while streaming $dataTypeName: ${e.message}" }
    throw e
} catch (e: Exception) {
    log.error(e) { "Error writing $dataTypeName" }
    throw e
}
