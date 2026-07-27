package org.genspectrum.lapis.request.converter

fun interface FieldConverter<T> {
    fun convert(source: String): T
}
