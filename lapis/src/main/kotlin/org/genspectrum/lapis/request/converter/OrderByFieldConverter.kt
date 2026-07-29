package org.genspectrum.lapis.request.converter

import org.genspectrum.lapis.request.Order
import org.genspectrum.lapis.request.OrderByField
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

/**
 * The `OrderByFieldConverter` converts a list of strings into a list of fields to order by.
 * It checks that all the fields exist, or start with "random".
 * The `Converter` is used automatically by Spring in the GET requests.
 */
@Component
class OrderByFieldConverter(
    private val orderByFieldsCleaner: OrderByFieldsCleaner,
) : Converter<String, OrderByField> {
    override fun convert(source: String): OrderByField {
        val field =
            if (source.startsWith("random")) {
                // validation and conversion happens later on, in `toOrderBySpec`.
                source
            } else {
                orderByFieldsCleaner.clean(source)
            }

        return OrderByField(field = field, order = Order.ASCENDING)
    }
}
