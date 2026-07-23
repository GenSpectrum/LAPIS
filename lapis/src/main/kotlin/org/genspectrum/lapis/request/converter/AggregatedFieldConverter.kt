package org.genspectrum.lapis.request.converter

import org.genspectrum.lapis.request.Field
import org.springframework.stereotype.Component

/** Resolves a field that may be a plain metadata field, sequence position field, or scalar function field. Used by `/aggregated`. */
@Component
class AggregatedFieldConverter(
    private val sequencePositionFieldConverter: SequencePositionFieldConverter,
    private val scalarFunctionFieldConverter: ScalarFunctionFieldConverter,
    private val metadataFieldConverter: MetadataFieldConverter,
) : FieldConverter<Field> {
    override fun convert(source: String): Field =
        sequencePositionFieldConverter.tryConvert(source)
            ?: scalarFunctionFieldConverter.tryConvert(source)
            ?: metadataFieldConverter.convert(source)
}
