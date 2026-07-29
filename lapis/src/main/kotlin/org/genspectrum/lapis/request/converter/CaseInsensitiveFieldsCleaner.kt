package org.genspectrum.lapis.request.converter

import org.genspectrum.lapis.config.ActiveView
import org.springframework.stereotype.Component

@Component
class CaseInsensitiveFieldsCleaner(
    private val activeView: ActiveView,
) {
    private val fieldsMap get() = activeView.databaseConfig.schema.metadata.map {
        it.name
    }.associateBy { it.lowercase() }

    fun clean(fieldName: String) = fieldsMap[fieldName.lowercase()]

    fun getKnownFields() = fieldsMap.values
}
