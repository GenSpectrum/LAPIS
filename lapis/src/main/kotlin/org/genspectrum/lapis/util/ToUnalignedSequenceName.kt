package org.genspectrum.lapis.util

const val UNALIGNED_PREFIX = "unaligned_"

fun toUnalignedSequenceName(sequenceName: String) = "$UNALIGNED_PREFIX$sequenceName"
