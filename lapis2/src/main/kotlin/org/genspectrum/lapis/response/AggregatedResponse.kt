package org.genspectrum.lapis.response

data class AggregatedResponse(val count: Int)

data class MutationProportion(val mutation: String, val count: Int, val proportion: Double)
