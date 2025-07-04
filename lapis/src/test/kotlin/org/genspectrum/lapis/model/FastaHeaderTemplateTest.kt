package org.genspectrum.lapis.model

import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.DatabaseMetadata
import org.genspectrum.lapis.config.DatabaseSchema
import org.genspectrum.lapis.config.MetadataType
import org.genspectrum.lapis.config.OpennessLevel
import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.request.CaseInsensitiveFieldsCleaner
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FastaHeaderTemplateTest {
    private val fastaHeaderTemplateParser = FastaHeaderTemplateParser(
        caseInsensitiveFieldsCleaner = CaseInsensitiveFieldsCleaner(
            databaseConfig = DatabaseConfig(
                schema = DatabaseSchema(
                    instanceName = "test",
                    opennessLevel = OpennessLevel.OPEN,
                    metadata = listOf(
                        DatabaseMetadata(name = "accession", type = MetadataType.STRING),
                        DatabaseMetadata(name = "age", type = MetadataType.INT),
                        DatabaseMetadata(name = "qc", type = MetadataType.FLOAT),
                        DatabaseMetadata(name = "isBoolean", type = MetadataType.BOOLEAN),
                        DatabaseMetadata(name = "date", type = MetadataType.DATE),
                        DatabaseMetadata(name = "primaryKey", type = MetadataType.STRING),
                    ),
                    primaryKey = "primaryKey",
                    features = emptyList(),
                ),
            ),
        ),
    )

    @Test
    fun `GIVEN simple template with one placeholder THEN is filled correctly`() {
        val template = fastaHeaderTemplateParser.parseTemplate(
            template = "accession: {accession}",
            sequenceSymbolType = SequenceSymbolType.NUCLEOTIDE,
        )

        val filledTemplate = template.fillTemplate(
            values = mapOf("accession" to TextNode("my_accession")),
            sequenceName = "sequenceName",
        )

        assertThat(filledTemplate, `is`("accession: my_accession"))
    }

    @Test
    fun `GIVEN template with placeholder in wrong case THEN is filled correctly`() {
        val template = fastaHeaderTemplateParser.parseTemplate(
            template = "isBoolean: {iSbOoLeAn}",
            sequenceSymbolType = SequenceSymbolType.NUCLEOTIDE,
        )

        val filledTemplate = template.fillTemplate(
            values = mapOf("isBoolean" to BooleanNode.TRUE),
            sequenceName = "sequenceName",
        )

        assertThat(filledTemplate, `is`("isBoolean: true"))
    }

    @Test
    fun `GIVEN template with multiple placeholders THEN is filled correctly`() {
        val template = fastaHeaderTemplateParser.parseTemplate(
            template = "accession: {accession}, age: {age}, qc: {qc}, isBoolean: {isBoolean}, date: {date}",
            sequenceSymbolType = SequenceSymbolType.NUCLEOTIDE,
        )

        val filledTemplate = template.fillTemplate(
            values = mapOf(
                "accession" to TextNode("my_accession"),
                "age" to IntNode(42),
                "qc" to DoubleNode(0.987),
                "isBoolean" to BooleanNode.TRUE,
                "date" to NullNode.instance,
            ),
            sequenceName = "sequenceName",
        )

        assertThat(
            filledTemplate,
            `is`("accession: my_accession, age: 42, qc: 0.987, isBoolean: true, date: null"),
        )
    }

    @Test
    fun `WHEN I fill the template THEN superfluous values are ignored`() {
        val template = fastaHeaderTemplateParser.parseTemplate(
            template = "accession: {accession}",
            sequenceSymbolType = SequenceSymbolType.NUCLEOTIDE,
        )

        val filledTemplate = template.fillTemplate(
            values = mapOf(
                "accession" to TextNode("my_accession"),
                "superfluous" to TextNode("should be ignored"),
            ),
            sequenceName = "sequenceName",
        )

        assertThat(filledTemplate, `is`("accession: my_accession"))
    }

    @Test
    fun `WHEN I fill the template THEN missing values should be filled with the field name`() {
        // The templates will be filled when streaming the sequences, so we must not fail if a value is missing.

        val template =
            fastaHeaderTemplateParser.parseTemplate(
                template = "accession: {accession}, age: {age}, qc: {qc}",
                sequenceSymbolType = SequenceSymbolType.NUCLEOTIDE,
            )

        val filledTemplate = template.fillTemplate(
            values = mapOf(
                "accession" to TextNode("my_accession"),
                // "age" is missing
                "qc" to DoubleNode(0.987),
            ),
            sequenceName = "sequenceName",
        )

        assertThat(filledTemplate, `is`("accession: my_accession, age: {age}, qc: 0.987"))
    }

    @Test
    fun `GIVEN multiple occurrences of the same field THEN is filled correctly`() {
        val template = fastaHeaderTemplateParser.parseTemplate(
            template = "{accession} {age} {accession}",
            sequenceSymbolType = SequenceSymbolType.NUCLEOTIDE,
        )

        val filledTemplate = template.fillTemplate(
            values = mapOf("accession" to TextNode("my_accession"), "age" to IntNode(42)),
            sequenceName = "sequenceName",
        )

        assertThat(filledTemplate, `is`("my_accession 42 my_accession"))
    }

    @Test
    fun `GIVEN template with segment placeholders THEN is filled correctly`() {
        val template = fastaHeaderTemplateParser.parseTemplate(
            template = "segment: {.segment}",
            sequenceSymbolType = SequenceSymbolType.NUCLEOTIDE,
        )

        val filledTemplate = template.fillTemplate(
            values = emptyMap(),
            sequenceName = "my_segment",
        )

        assertThat(filledTemplate, `is`("segment: my_segment"))
    }

    @Test
    fun `GIVEN template with segment placeholder with different casing THEN is filled correctly`() {
        val template = fastaHeaderTemplateParser.parseTemplate(
            template = "segment: {.sEgMeNt}",
            sequenceSymbolType = SequenceSymbolType.NUCLEOTIDE,
        )

        val filledTemplate = template.fillTemplate(
            values = emptyMap(),
            sequenceName = "my_segment",
        )

        assertThat(filledTemplate, `is`("segment: my_segment"))
    }

    @Test
    fun `GIVEN template with gene placeholders THEN is filled correctly`() {
        val template = fastaHeaderTemplateParser.parseTemplate(
            template = "gene: {.gene}",
            sequenceSymbolType = SequenceSymbolType.AMINO_ACID,
        )

        val filledTemplate = template.fillTemplate(
            values = emptyMap(),
            sequenceName = "my_gene",
        )

        assertThat(filledTemplate, `is`("gene: my_gene"))
    }

    @Test
    fun `GIVEN template with gene placeholder with different casing THEN is filled correctly`() {
        val template = fastaHeaderTemplateParser.parseTemplate(
            template = "gene: {.gEnE}",
            sequenceSymbolType = SequenceSymbolType.AMINO_ACID,
        )

        val filledTemplate = template.fillTemplate(
            values = emptyMap(),
            sequenceName = "my_gene",
        )

        assertThat(filledTemplate, `is`("gene: my_gene"))
    }

    @Test
    fun `GIVEN template with unknown metadata field THEN throws exception`() {
        val exception = assertThrows<BadRequestException> {
            fastaHeaderTemplateParser.parseTemplate(
                template = "{unknown field}",
                sequenceSymbolType = SequenceSymbolType.AMINO_ACID,
            )
        }
        assertThat(
            exception.message,
            containsString("Invalid FASTA header template: 'unknown field' is not a valid metadata field."),
        )
    }

    @Test
    fun `GIVEN nucleotide template with gene field THEN throws exception`() {
        val exception = assertThrows<BadRequestException> {
            fastaHeaderTemplateParser.parseTemplate(
                template = "{.gene}",
                sequenceSymbolType = SequenceSymbolType.NUCLEOTIDE,
            )
        }
        assertThat(
            exception.message,
            containsString(
                "Invalid FASTA header template: '.gene' is only valid for amino acid sequences.",
            ),
        )
    }

    @Test
    fun `GIVEN amino acid template with segment fielt THEN throws exception`() {
        val exception = assertThrows<BadRequestException> {
            fastaHeaderTemplateParser.parseTemplate(
                template = "{.segment}",
                sequenceSymbolType = SequenceSymbolType.AMINO_ACID,
            )
        }
        assertThat(
            exception.message,
            containsString(
                "Invalid FASTA header template: '.segment' is only valid for nucleotide sequences.",
            ),
        )
    }
}
