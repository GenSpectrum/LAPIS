package org.genspectrum.lapis.model

import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode
import org.genspectrum.lapis.model.TemplateField.MetadataField
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

class FastaHeaderTemplateTest {
    private val fastaHeaderTemplateParser = FastaHeaderTemplateParser()

    @Test
    fun `GIVEN simple template with one placeholder THEN is filled correctly`() {
        val template = fastaHeaderTemplateParser.parseTemplate(template = "accession: {accession}")

        assertThat(template.metadataFields, `is`(listOf(MetadataField("accession"))))

        val filledTemplate = template.fillTemplate(
            values = mapOf("accession" to TextNode("my_accession")),
            sequenceName = null,
        )

        assertThat(filledTemplate, `is`("accession: my_accession"))
    }

    @Test
    fun `GIVEN template with multiple placeholders THEN is filled correctly`() {
        val template = fastaHeaderTemplateParser.parseTemplate(
            template = "accession: {accession}, age: {age}, qc: {qc}, isBoolean: {isBoolean}, date: {date}",
        )

        assertThat(
            template.metadataFields,
            `is`(
                listOf(
                    MetadataField("accession"),
                    MetadataField("age"),
                    MetadataField("qc"),
                    MetadataField("isBoolean"),
                    MetadataField("date"),
                ),
            ),
        )

        val filledTemplate = template.fillTemplate(
            values = mapOf(
                "accession" to TextNode("my_accession"),
                "age" to IntNode(42),
                "qc" to DoubleNode(0.987),
                "isBoolean" to BooleanNode.TRUE,
                "date" to NullNode.instance,
            ),
            sequenceName = null,
        )

        assertThat(
            filledTemplate,
            `is`("accession: my_accession, age: 42, qc: 0.987, isBoolean: true, date: null"),
        )
    }

    @Test
    fun `WHEN I fill the template THEN superfluous values are ignored`() {
        val template = fastaHeaderTemplateParser.parseTemplate(template = "accession: {accession}")

        val filledTemplate = template.fillTemplate(
            values = mapOf(
                "accession" to TextNode("my_accession"),
                "superfluous" to TextNode("should be ignored"),
            ),
            sequenceName = null,
        )

        assertThat(filledTemplate, `is`("accession: my_accession"))
    }

    @Test
    fun `WHEN I fill the template THEN missing values should be filled with the field name`() {
        // The templates will be filled when streaming the sequences, so we must not fail if a value is missing.

        val template =
            fastaHeaderTemplateParser.parseTemplate(template = "accession: {accession}, age: {age}, qc: {qc}")

        val filledTemplate = template.fillTemplate(
            values = mapOf(
                "accession" to TextNode("my_accession"),
                // "age" is missing
                "qc" to DoubleNode(0.987),
            ),
            sequenceName = null,
        )

        assertThat(filledTemplate, `is`("accession: my_accession, age: {age}, qc: 0.987"))
    }

    @Test
    fun `GIVEN multiple occurrences of the same field THEN is filled correctly`() {
        val template = fastaHeaderTemplateParser.parseTemplate(template = "{accession} {age} {accession}")

        assertThat(template.metadataFields, `is`(listOf(MetadataField("accession"), MetadataField("age"))))

        val filledTemplate = template.fillTemplate(
            values = mapOf("accession" to TextNode("my_accession"), "age" to IntNode(42)),
            sequenceName = null,
        )

        assertThat(filledTemplate, `is`("my_accession 42 my_accession"))
    }

    @Test
    fun `GIVEN template with segment placeholders THEN is filled correctly`() {
        val template = fastaHeaderTemplateParser.parseTemplate(template = "segment: {.segment}")

        assertThat(template.metadataFields, `is`(emptyList()))

        val filledTemplate = template.fillTemplate(
            values = emptyMap(),
            sequenceName = "my_segment",
        )

        assertThat(filledTemplate, `is`("segment: my_segment"))
    }

    @Test
    fun `GIVEN template with gene placeholders THEN is filled correctly`() {
        val template = fastaHeaderTemplateParser.parseTemplate(template = "gene: {.gene}")

        assertThat(template.metadataFields, `is`(emptyList()))

        val filledTemplate = template.fillTemplate(
            values = emptyMap(),
            sequenceName = "my_gene",
        )

        assertThat(filledTemplate, `is`("gene: my_gene"))
    }
}
