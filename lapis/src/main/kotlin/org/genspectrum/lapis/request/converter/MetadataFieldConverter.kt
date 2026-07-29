package org.genspectrum.lapis.request.converter

import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.request.PlainField
import org.springframework.stereotype.Component

@Component
class MetadataFieldConverter(
    private val caseInsensitiveFieldsCleaner: CaseInsensitiveFieldsCleaner,
) : FieldConverter<PlainField> {
    override fun convert(source: String): PlainField {
        val cleaned = caseInsensitiveFieldsCleaner.clean(source)
            ?: throw BadRequestException(
                "Unknown field: '$source', known values are ${caseInsensitiveFieldsCleaner.getKnownFields()}",
            )
        return PlainField(cleaned)
    }
}
