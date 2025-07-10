package org.genspectrum.lapis.model

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.request.CaseInsensitiveFieldsCleaner
import org.springframework.stereotype.Component
import java.util.Locale

private const val SEGMENT_PLACEHOLDER = ".segment"
private const val GENE_PLACEHOLDER = ".gene"

data class FastaHeaderTemplate(
    private val templateString: String,
    private val fields: Set<TemplateField>,
) {
    val metadataFieldNames = fields
        .filterIsInstance<TemplateField.MetadataField>()
        .map { it.fieldNameInConfig }

    fun fillTemplate(
        values: Map<String, JsonNode>,
        sequenceName: String,
    ): String {
        var result = templateString

        fields
            .map { field ->
                when (field) {
                    is TemplateField.MetadataField -> field.fieldNameInTemplate to values[field.fieldNameInConfig]
                    TemplateField.GeneField -> GENE_PLACEHOLDER to TextNode(sequenceName)
                    TemplateField.SegmentField -> SEGMENT_PLACEHOLDER to TextNode(sequenceName)
                }
            }
            .filter { (_, value) -> value != null }
            .forEach { (field, value) ->
                result = result.replace("{$field}", value!!.asText(), ignoreCase = true)
            }

        return result
    }
}

sealed interface TemplateField {
    data class MetadataField(
        val fieldNameInTemplate: String,
        val fieldNameInConfig: String,
    ) : TemplateField

    data object SegmentField : TemplateField

    data object GeneField : TemplateField
}

@Component
class FastaHeaderTemplateParser(
    private val caseInsensitiveFieldsCleaner: CaseInsensitiveFieldsCleaner,
) {
    companion object {
        private val FIELD_REGEX = Regex("""\{([^}]+)}""")
    }

    fun parseTemplate(
        template: String,
        sequenceSymbolType: SequenceSymbolType,
    ): FastaHeaderTemplate {
        val templateFields = FIELD_REGEX.findAll(template)
            .map { matchResult ->
                when (val fieldName = matchResult.groupValues[1].lowercase(Locale.US)) {
                    SEGMENT_PLACEHOLDER -> {
                        if (sequenceSymbolType != SequenceSymbolType.NUCLEOTIDE) {
                            throw BadRequestException(
                                "Invalid FASTA header template: '$SEGMENT_PLACEHOLDER' is only valid for nucleotide sequences.",
                            )
                        }
                        TemplateField.SegmentField
                    }

                    GENE_PLACEHOLDER -> {
                        if (sequenceSymbolType != SequenceSymbolType.AMINO_ACID) {
                            throw BadRequestException(
                                "Invalid FASTA header template: '$GENE_PLACEHOLDER' is only valid for amino acid sequences.",
                            )
                        }
                        TemplateField.GeneField
                    }

                    else -> TemplateField.MetadataField(
                        fieldNameInTemplate = fieldName,
                        fieldNameInConfig = caseInsensitiveFieldsCleaner.clean(fieldName)
                            ?: throw invalidFieldException(fieldName, sequenceSymbolType),
                    )
                }
            }
            .toSet()

        return FastaHeaderTemplate(template, templateFields)
    }

    private fun invalidFieldException(
        fieldName: String,
        sequenceSymbolType: SequenceSymbolType,
    ): BadRequestException =
        BadRequestException(
            "Invalid FASTA header template: '$fieldName' is not a valid metadata field. " +
                "Available fields: ${caseInsensitiveFieldsCleaner.getKnownFields().joinToString(", ")}. " +
                when (sequenceSymbolType) {
                    SequenceSymbolType.NUCLEOTIDE -> "Use {$SEGMENT_PLACEHOLDER} as a placeholder for segment name."
                    SequenceSymbolType.AMINO_ACID -> "Use {$GENE_PLACEHOLDER} as a placeholder for gene name."
                },
        )
}
