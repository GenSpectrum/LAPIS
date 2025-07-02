package org.genspectrum.lapis.model

import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.databind.node.ValueNode
import org.springframework.stereotype.Component

data class FastaHeaderTemplate(
    private val templateString: String,
    private val fields: Set<TemplateField>,
) {
    val metadataFields = fields.filterIsInstance<TemplateField.MetadataField>()

    val hasGeneField = fields.any { it is TemplateField.GeneField }

    val hasSegmentField = fields.any { it is TemplateField.SegmentField }

    fun fillTemplate(
        values: Map<String, ValueNode>,
        sequenceName: String?,
    ): String {
        var result = templateString

        fields
            .map { field ->
                when (field) {
                    is TemplateField.MetadataField -> field.fieldName to values[field.fieldName]
                    TemplateField.GeneField -> GENE_PLACEHOLDER to sequenceName?.let { TextNode(it) }
                    TemplateField.SegmentField -> SEGMENT_PLACEHOLDER to sequenceName?.let { TextNode(it) }
                }
            }
            .filter { (_, value) -> value != null }
            .forEach { (field, value) ->
                result = result.replace("{$field}", value!!.asText())
            }

        values.forEach { (field, value) ->
            result = result.replace("{$field}", value.asText())
        }
        return result
    }
}

sealed interface TemplateField {
    data class MetadataField(
        val fieldName: String,
    ) : TemplateField

    data object SegmentField : TemplateField

    data object GeneField : TemplateField
}

private const val SEGMENT_PLACEHOLDER = ".segment"
private const val GENE_PLACEHOLDER = ".gene"

@Component
class FastaHeaderTemplateParser {
    companion object {
        private val FIELD_REGEX = Regex("""\{([^}]+)}""")
    }

    fun parseTemplate(template: String): FastaHeaderTemplate {
        val templateFields = FIELD_REGEX.findAll(template)
            .map { matchResult ->
                when (val fieldName = matchResult.groupValues[1]) {
                    SEGMENT_PLACEHOLDER -> TemplateField.SegmentField
                    GENE_PLACEHOLDER -> TemplateField.GeneField
                    else -> TemplateField.MetadataField(fieldName = fieldName)
                }
            }
            .toSet()

        return FastaHeaderTemplate(template, templateFields)
    }
}
