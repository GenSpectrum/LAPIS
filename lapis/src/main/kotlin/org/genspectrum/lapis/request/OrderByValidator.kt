package org.genspectrum.lapis.request

import org.genspectrum.lapis.controller.BadRequestException

object OrderByValidator {
    fun validateAndConvert(orderByFields: List<OrderByField>?): OrderBySpec {
        if (orderByFields == null || orderByFields.isEmpty()) {
            return OrderBySpec.ByFields(emptyList())
        }

        val hasRandom = orderByFields.any { it.field.startsWith("random") }

        if (hasRandom && orderByFields.size > 1) {
            throw BadRequestException(
                "Cannot mix 'random' with other orderBy fields. " +
                    "Use either 'orderBy=random' or 'orderBy=field1,field2'",
            )
        }

        return orderByFields.toOrderBySpec()
    }
}
