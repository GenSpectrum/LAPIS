package org.genspectrum.lapis.silonew

sealed class SiloRelation : SiloExpression()

private class PipelineOperation(
    private val input: SiloRelation,
    private val name: String,
    private val positionalArguments: List<SiloExpression>,
    private val namedArguments: List<SiloNamedArgument>,
) : SiloRelation() {
    override val precedence = POSTFIX_PRECEDENCE

    override fun renderSelf() = renderCall(input, name, positionalArguments, namedArguments)
}

private class RelationFunction(
    private val name: String,
    private val positionalArguments: List<SiloExpression>,
    private val namedArguments: List<SiloNamedArgument>,
) : SiloRelation() {
    override val precedence = POSTFIX_PRECEDENCE

    override fun renderSelf() = renderCall(name, positionalArguments, namedArguments)
}

/** Builds a relation-returning function from a name and structured, escaped arguments. */
fun relationCall(
    name: String,
    positionalArguments: List<SiloExpression> = emptyList(),
    namedArguments: List<SiloNamedArgument> = emptyList(),
): SiloRelation =
    RelationFunction(
        name = name,
        positionalArguments = positionalArguments,
        namedArguments = namedArguments,
    )

/** Appends a relation-returning pipeline operation from a name and structured, escaped arguments. */
fun SiloRelation.pipe(
    name: String,
    positionalArguments: List<SiloExpression> = emptyList(),
    namedArguments: List<SiloNamedArgument> = emptyList(),
): SiloRelation =
    PipelineOperation(
        input = this,
        name = name,
        positionalArguments = positionalArguments,
        namedArguments = namedArguments,
    )

fun SiloRelation.filter(predicate: SiloExpression): SiloRelation =
    pipe(name = "filter", positionalArguments = listOf(predicate))

fun SiloRelation.project(vararg fields: SiloField): SiloRelation = project(fields.asIterable())

fun SiloRelation.project(fields: Iterable<SiloField>): SiloRelation =
    pipe(name = "project", positionalArguments = listOf(set(fields)))

fun SiloRelation.map(vararg assignments: SiloAssignment): SiloRelation = map(assignments.asIterable())

fun SiloRelation.map(assignments: Iterable<SiloAssignment>): SiloRelation =
    pipe(name = "map", positionalArguments = listOf(record(assignments)))

fun SiloRelation.groupBy(
    aggregates: SiloRecord,
    columns: SiloSet? = null,
): SiloRelation = pipe(name = "groupBy", positionalArguments = listOfNotNull(aggregates, columns))

fun SiloRelation.orderBy(vararg expressions: SiloExpression): SiloRelation = orderBy(expressions.asIterable())

fun SiloRelation.orderBy(expressions: Iterable<SiloExpression>): SiloRelation =
    pipe(name = "orderBy", positionalArguments = listOf(set(expressions)))

fun SiloRelation.limit(count: Int): SiloRelation = pipe(name = "limit", positionalArguments = listOf(literal(count)))

fun SiloRelation.limit(count: Long): SiloRelation = pipe(name = "limit", positionalArguments = listOf(literal(count)))

fun SiloRelation.offset(count: Int): SiloRelation = pipe(name = "offset", positionalArguments = listOf(literal(count)))

fun SiloRelation.offset(count: Long): SiloRelation = pipe(name = "offset", positionalArguments = listOf(literal(count)))

fun SiloRelation.randomize(seed: Int? = null): SiloRelation =
    pipe(
        name = "randomize",
        namedArguments = seed?.let { listOf(namedArgument("seed", literal(it))) }.orEmpty(),
    )

fun SiloRelation.mutations(
    minProportion: Double? = null,
    sequenceNames: Iterable<SiloField>? = null,
    fields: Iterable<SiloField>? = null,
): SiloRelation =
    specializedMutationOperation(
        name = "mutations",
        minProportion = minProportion,
        sequenceNames = sequenceNames,
        fields = fields,
    )

fun SiloRelation.aminoAcidMutations(
    minProportion: Double? = null,
    sequenceNames: Iterable<SiloField>? = null,
    fields: Iterable<SiloField>? = null,
): SiloRelation =
    specializedMutationOperation(
        name = "aminoAcidMutations",
        minProportion = minProportion,
        sequenceNames = sequenceNames,
        fields = fields,
    )

private fun SiloRelation.specializedMutationOperation(
    name: String,
    minProportion: Double?,
    sequenceNames: Iterable<SiloField>?,
    fields: Iterable<SiloField>?,
) = pipe(
    name = name,
    namedArguments = buildList {
        minProportion?.let { add(namedArgument("minProportion", literal(it))) }
        sequenceNames?.let { add(namedArgument("sequenceNames", set(it))) }
        fields?.let { add(namedArgument("fields", set(it))) }
    },
)

fun SiloRelation.insertions(sequenceNames: Iterable<SiloField>? = null): SiloRelation =
    sequenceOperation(name = "insertions", sequenceNames = sequenceNames)

fun SiloRelation.aminoAcidInsertions(sequenceNames: Iterable<SiloField>? = null): SiloRelation =
    sequenceOperation(name = "aminoAcidInsertions", sequenceNames = sequenceNames)

private fun SiloRelation.sequenceOperation(
    name: String,
    sequenceNames: Iterable<SiloField>?,
): SiloRelation =
    pipe(
        name = name,
        namedArguments = sequenceNames?.let { listOf(namedArgument("sequenceNames", set(it))) }.orEmpty(),
    )

fun SiloRelation.mostRecentCommonAncestor(
    column: SiloField,
    printNodesNotInTree: Boolean? = null,
): SiloRelation =
    pipe(
        name = "mostRecentCommonAncestor",
        positionalArguments = listOf(literal(column.name)),
        namedArguments = printNodesNotInTree
            ?.let { listOf(namedArgument("printNodesNotInTree", literal(it))) }
            .orEmpty(),
    )

fun SiloRelation.phyloSubtree(
    column: SiloField,
    printNodesNotInTree: Boolean? = null,
    contractUnaryNodes: Boolean? = null,
): SiloRelation =
    pipe(
        name = "phyloSubtree",
        positionalArguments = listOf(literal(column.name)),
        namedArguments = buildList {
            printNodesNotInTree?.let { add(namedArgument("printNodesNotInTree", literal(it))) }
            contractUnaryNodes?.let { add(namedArgument("contractUnaryNodes", literal(it))) }
        },
    )

fun SiloRelation.schema(): SiloRelation = pipe("schema")

fun unionAll(
    left: SiloRelation,
    right: SiloRelation,
): SiloRelation = relationCall(name = "unionAll", positionalArguments = listOf(left, right))

/** The join variants supported by SILO. */
enum class JoinType(
    internal val token: String,
) {
    INNER("inner"),
    LEFT("left"),
    RIGHT("right"),
    FULL("full"),
    LEFT_SEMI("leftSemi"),
    RIGHT_SEMI("rightSemi"),
    LEFT_ANTI("leftAnti"),
    RIGHT_ANTI("rightAnti"),
}

private class JoinTypeExpression(
    private val type: JoinType,
) : SiloExpression() {
    override val precedence = ATOMIC_PRECEDENCE

    override fun renderSelf() = type.token
}

/** Joins two relations using [on] and the selected SILO [type]. */
fun join(
    left: SiloRelation,
    right: SiloRelation,
    on: SiloExpression,
    type: JoinType = JoinType.INNER,
): SiloRelation =
    relationCall(
        name = "join",
        positionalArguments = listOf(left, right, on),
        namedArguments = listOf(namedArgument("type", JoinTypeExpression(type))),
    )

/** Joins this relation with [right] using [on] and the selected SILO [type]. */
@JvmName("joinRelation")
fun SiloRelation.join(
    right: SiloRelation,
    on: SiloExpression,
    type: JoinType = JoinType.INNER,
): SiloRelation =
    pipe(
        name = "join",
        positionalArguments = listOf(right, on),
        namedArguments = listOf(namedArgument("type", JoinTypeExpression(type))),
    )

fun asc(field: SiloField): SiloExpression = call(name = "asc", positionalArguments = listOf(field))

fun desc(field: SiloField): SiloExpression = call(name = "desc", positionalArguments = listOf(field))

fun count(): SiloExpression = call("count")

fun SiloExpression.at(position: Int): SiloExpression =
    call(name = "at", positionalArguments = listOf(literal(position)))

fun SiloExpression.isoWeek(): SiloExpression = call("isoWeek")

fun SiloExpression.between(
    from: SiloExpression,
    to: SiloExpression,
): SiloExpression = call(name = "between", positionalArguments = listOf(from, to))

fun SiloExpression.isIn(vararg values: SiloExpression): SiloExpression =
    call(name = "in", positionalArguments = listOf(set(*values)))

fun SiloExpression.isNull(): SiloExpression = functionCall(name = "isNull", positionalArguments = listOf(this))

fun SiloExpression.isNotNull(): SiloExpression = functionCall(name = "isNotNull", positionalArguments = listOf(this))

fun SiloExpression.like(pattern: String): SiloExpression =
    call(name = "like", positionalArguments = listOf(literal(pattern)))

fun SiloExpression.lineage(
    value: String?,
    includeSublineages: Boolean? = null,
    recombinantFollowingMode: String? = null,
): SiloExpression =
    call(
        name = "lineage",
        positionalArguments = listOf(value?.let(::literal) ?: nullLiteral()),
        namedArguments = buildList {
            includeSublineages?.let { add(namedArgument("includeSublineages", literal(it))) }
            recombinantFollowingMode?.let { add(namedArgument("recombinantFollowingMode", literal(it))) }
        },
    )

fun SiloExpression.phyloDescendantOf(node: String): SiloExpression =
    call(name = "phyloDescendantOf", positionalArguments = listOf(literal(node)))

fun nucleotideEquals(
    position: Int,
    symbol: String,
    sequenceName: String? = null,
): SiloExpression =
    sequencePredicate(
        name = "nucleotideEquals",
        position = position,
        symbol = symbol,
        sequenceName = sequenceName,
    )

fun aminoAcidEquals(
    position: Int,
    symbol: String,
    sequenceName: String? = null,
): SiloExpression =
    sequencePredicate(
        name = "aminoAcidEquals",
        position = position,
        symbol = symbol,
        sequenceName = sequenceName,
    )

private fun sequencePredicate(
    name: String,
    position: Int,
    symbol: String,
    sequenceName: String?,
) = call(
    name = name,
    namedArguments = buildList {
        add(namedArgument("position", literal(position)))
        add(namedArgument("symbol", literal(symbol)))
        sequenceName?.let { add(namedArgument("sequenceName", literal(it))) }
    },
)

fun hasMutation(
    position: Int,
    sequenceName: String? = null,
): SiloExpression = positionPredicate(name = "hasMutation", position = position, sequenceName = sequenceName)

fun hasAminoAcidMutation(
    position: Int,
    sequenceName: String? = null,
): SiloExpression = positionPredicate(name = "hasAAMutation", position = position, sequenceName = sequenceName)

private fun positionPredicate(
    name: String,
    position: Int,
    sequenceName: String?,
) = call(
    name = name,
    namedArguments = buildList {
        add(namedArgument("position", literal(position)))
        sequenceName?.let { add(namedArgument("sequenceName", literal(it))) }
    },
)

fun insertionContains(
    position: Int,
    value: String,
    sequenceName: String? = null,
): SiloExpression =
    insertionPredicate(
        name = "insertionContains",
        position = position,
        value = value,
        sequenceName = sequenceName,
    )

fun aminoAcidInsertionContains(
    position: Int,
    value: String,
    sequenceName: String? = null,
): SiloExpression =
    insertionPredicate(
        name = "aminoAcidInsertionContains",
        position = position,
        value = value,
        sequenceName = sequenceName,
    )

private fun insertionPredicate(
    name: String,
    position: Int,
    value: String,
    sequenceName: String?,
) = call(
    name = name,
    namedArguments = buildList {
        add(namedArgument("position", literal(position)))
        add(namedArgument("value", literal(value)))
        sequenceName?.let { add(namedArgument("sequenceName", literal(it))) }
    },
)

fun maybe(child: SiloExpression): SiloExpression = call(name = "maybe", positionalArguments = listOf(child))

fun exact(child: SiloExpression): SiloExpression = call(name = "exact", positionalArguments = listOf(child))

fun nOf(
    count: Int,
    children: Iterable<SiloExpression>,
    matchExactly: Boolean? = null,
): SiloExpression =
    call(
        name = "nOf",
        positionalArguments = listOf(literal(count), set(children)),
        namedArguments = matchExactly?.let { listOf(namedArgument("matchExactly", literal(it))) }.orEmpty(),
    )

fun mutation(
    position: Int,
    symbol: String,
): SiloRecord =
    record(
        assignment("position", literal(position)),
        assignment("symbol", literal(symbol)),
    )

fun nucleotideMutationProfile(
    distance: Int,
    sequenceName: String? = null,
    querySequence: String? = null,
    sequenceId: String? = null,
    mutations: Iterable<SiloRecord>? = null,
): SiloExpression =
    mutationProfile(
        name = "nucleotideMutationProfile",
        distance = distance,
        sequenceName = sequenceName,
        querySequence = querySequence,
        sequenceId = sequenceId,
        mutations = mutations,
    )

fun aminoAcidMutationProfile(
    distance: Int,
    sequenceName: String? = null,
    querySequence: String? = null,
    sequenceId: String? = null,
    mutations: Iterable<SiloRecord>? = null,
): SiloExpression =
    mutationProfile(
        name = "aminoAcidMutationProfile",
        distance = distance,
        sequenceName = sequenceName,
        querySequence = querySequence,
        sequenceId = sequenceId,
        mutations = mutations,
    )

private fun mutationProfile(
    name: String,
    distance: Int,
    sequenceName: String?,
    querySequence: String?,
    sequenceId: String?,
    mutations: Iterable<SiloRecord>?,
) = call(
    name = name,
    namedArguments = buildList {
        add(namedArgument("distance", literal(distance)))
        sequenceName?.let { add(namedArgument("sequenceName", literal(it))) }
        querySequence?.let { add(namedArgument("querySequence", literal(it))) }
        sequenceId?.let { add(namedArgument("sequenceId", literal(it))) }
        mutations?.let { add(namedArgument("mutations", set(it))) }
    },
)
