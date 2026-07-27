package org.genspectrum.lapis.request.converter

import org.genspectrum.lapis.request.Field
import org.springframework.stereotype.Component

/** Resolves a field that may be a plain metadata field or a sequence position field. Used by `/aggregated`. */
@Component
class AggregatedFieldConverter(
    private val sequencePositionFieldConverter: SequencePositionFieldConverter,
    private val metadataFieldConverter: MetadataFieldConverter,
) : FieldConverter<Field> {
    override fun convert(source: String): Field =
        sequencePositionFieldConverter.tryConvert(source) ?: metadataFieldConverter.convert(source)
}
