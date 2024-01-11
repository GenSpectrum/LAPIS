package org.genspectrum.lapis.request

interface MaybeMutation<Self : MaybeMutation<Self>> {
    val maybe: Boolean

    fun asMaybe(): Self
}

val MAYBE_REGEX = Regex("""^MAYBE\((?<mutationCandidate>.+)\)$""")

inline fun <reified T : MaybeMutation<T>> wrapWithMaybeMutationParser(
    mutationCandidate: String,
    mutationParser: (String) -> T,
) = when (val match = MAYBE_REGEX.find(mutationCandidate)) {
    null -> mutationParser(mutationCandidate)
    else -> mutationParser(match.groups["mutationCandidate"]!!.value).asMaybe()
}
