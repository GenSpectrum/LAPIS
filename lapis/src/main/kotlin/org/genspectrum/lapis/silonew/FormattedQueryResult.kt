package org.genspectrum.lapis.silonew

import java.io.OutputStream
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A single-use, formatted SILO response.
 *
 * [writeTo] streams the response to an output and may only be called once. Closing the result releases all HTTP and
 * Arrow resources.
 */
class FormattedQueryResult internal constructor(
    private val resources: ArrowQueryResources,
    private val format: SiloDataFormat,
    val dataVersion: String?,
    headers: Map<String, List<String>>,
) : AutoCloseable {
    val headers: Map<String, List<String>> = Collections.unmodifiableMap(
        headers.mapValues { (_, values) -> Collections.unmodifiableList(values.toList()) },
    )
    val contentType: String = format.contentType

    private val consumptionStarted = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)

    /** Writes the complete formatted response, flushes [output], and leaves [output] open. */
    fun writeTo(output: OutputStream) {
        beginConsumption()
        var failure: Throwable? = null
        try {
            encodeArrowResult(resources.reader, format, output)
            output.flush()
        } catch (exception: Throwable) {
            failure = normalizeEncodingFailure(exception)
            throw failure
        } finally {
            closeResources(failure)
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) resources.close()
    }

    private fun beginConsumption() {
        check(!closed.get()) { "The formatted SILO result is closed" }
        check(consumptionStarted.compareAndSet(false, true)) { "The formatted SILO result can only be consumed once" }
    }

    private fun closeResources(originalFailure: Throwable?) {
        try {
            resources.close()
        } catch (closeFailure: Throwable) {
            if (originalFailure != null) {
                originalFailure.addSuppressed(closeFailure)
            } else {
                throw closeFailure
            }
        }
    }
}

private fun normalizeEncodingFailure(exception: Throwable): Throwable =
    when (exception) {
        is Error, is SiloClientException -> exception
        else -> SiloResultEncodingException("Could not encode the SILO result", exception)
    }
