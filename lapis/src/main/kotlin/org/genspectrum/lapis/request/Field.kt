package org.genspectrum.lapis.request

sealed interface Field {
    val outputColumnName: String
}

data class PlainField(
    val fieldName: String,
) : Field {
    override val outputColumnName: String get() = fieldName
}

data class SequencePositionField(
    val sequenceName: String,
    val position: Int,
    val isSingleSegment: Boolean = false,
) : Field {
    /** Used both as the SaneQL alias and as the response column key, e.g. `S[501]` or `[501]` for shorthand. */
    override val outputColumnName: String get() = if (isSingleSegment) "[$position]" else "$sequenceName[$position]"
}
