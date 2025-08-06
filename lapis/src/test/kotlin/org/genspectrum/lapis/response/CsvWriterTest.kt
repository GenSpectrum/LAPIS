package org.genspectrum.lapis.response

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test
import java.io.StringWriter
import java.util.stream.Stream

class CsvWriterTest {
    private val csvWriter = CsvWriter()
    private val ianaTsvWriter = IanaTsvWriter()

    // Dummy RecordCollection for testing
    private class TestRecordCollection(
        private val headers: List<String>,
        override val records: Stream<List<String?>>,
    ) : RecordCollection<List<String?>> {
        override fun getHeader(): List<String> = headers

        override fun mapToCsvValuesList(value: List<String?>): List<String?> = value
    }

    @Test
    fun `GIVEN data with nulls and special chars WHEN using the CsvWriter THEN output quotes fields`() {
        val data = TestRecordCollection(
            headers = listOf("col1", "col2"),
            records = listOf(
                listOf("val1", null),
                listOf("hello\nworld", "tab\tchar"),
            ).stream(),
        )
        val out = StringWriter()

        csvWriter.write(
            appendable = out,
            includeHeaders = true,
            data = data,
            delimiter = Delimiter.TAB,
        )

        val expected = buildString {
            appendLine("col1\tcol2")
            appendLine("val1\t")
            appendLine("\"hello\nworld\"\t\"tab\tchar\"")
        }
        assertThat(out.toString(), `is`(expected))
    }

    @Test
    fun `GIVEN data with nulls and special chars WHEN using the IanaTsvWriter THEN special chars are escaped`() {
        val data = TestRecordCollection(
            headers = listOf("col1", "col2"),
            records = listOf(
                listOf("val1", null),
                listOf("hello\nworld", "tab\tchar"),
            ).stream(),
        )
        val out = StringWriter()

        ianaTsvWriter.write(
            appendable = out,
            includeHeaders = true,
            data = data,
            delimiter = Delimiter.TAB,
        )

        val expected = buildString {
            appendLine("col1\tcol2")
            appendLine("val1\t")
            appendLine("hello\\nworld\ttab\\tchar")
        }
        assertThat(out.toString(), `is`(expected))
    }
}
