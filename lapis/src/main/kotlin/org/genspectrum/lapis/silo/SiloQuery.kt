package org.genspectrum.lapis.silo

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.genspectrum.lapis.request.Order
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.request.OrderBySpec
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.DetailsData
import org.genspectrum.lapis.response.InsertionData
import org.genspectrum.lapis.response.MostCommonAncestorData
import org.genspectrum.lapis.response.MutationData
import org.genspectrum.lapis.response.PhyloSubtreeData
import org.genspectrum.lapis.response.SequenceData
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.annotation.JsonSerialize
import java.time.LocalDate

data class SiloQuery<ResponseType>(
    val action: SiloAction<ResponseType>,
    val filterExpression: SiloFilterExpression,
) {
    /** Renders this query as a SaneQL query string, e.g. `default.filter(true).groupBy({count:=count()})`. */
    fun toSaneQl(): String = SaneQlPipeline(filterExpression.toSaneQl(), action.toSaneQlSteps()).render()
}

interface CommonActionFields {
    val orderByFields: List<OrderByField>
    val limit: Int?
    val offset: Int?
    val randomize: RandomizeConfig?
}

const val ORDER_BY_RANDOM_FIELD_NAME = "random"

sealed class SiloAction<ResponseType>(
    @JsonIgnore val arrowConverter: ArrowRowConverter<ResponseType>,
    @JsonIgnore val cacheable: Boolean,
) : CommonActionFields {
    /** The SaneQL pipeline step(s) specific to this action, e.g. `.groupBy({count:=count()})`. */
    protected abstract fun ownSaneQlSteps(): List<SaneQlStep>

    /**
     * All SaneQL pipeline steps for this action: [ownSaneQlSteps] followed by the steps common to
     * every action (`orderBy`, `offset`, `randomize`, `limit`), derived from [CommonActionFields].
     */
    fun toSaneQlSteps(): List<SaneQlStep> = ownSaneQlSteps() + commonSaneQlSuffixSteps()

    private fun commonSaneQlSuffixSteps(): List<SaneQlStep> =
        buildList {
            val orderByFields = this@SiloAction.orderByFields
            if (orderByFields.isNotEmpty()) {
                add(SaneQlStep("orderBy", positionalArgs = listOf(SaneQlList(orderByFields.map { toSaneQl(it) }))))
            }
            val offset = this@SiloAction.offset
            if (offset != null) {
                add(SaneQlStep("offset", positionalArgs = listOf(SaneQlInt(offset))))
            }
            when (val randomize = this@SiloAction.randomize) {
                is RandomizeConfig.Enabled -> {
                    add(SaneQlStep("randomize"))
                }

                is RandomizeConfig.WithSeed -> {
                    add(
                        SaneQlStep(
                            "randomize",
                            namedArgs = listOf(SaneQlNamedArg("seed", SaneQlInt(randomize.seed))),
                        ),
                    )
                }

                is RandomizeConfig.Disabled, null -> {}
            }
            val limit = this@SiloAction.limit
            if (limit != null) {
                add(SaneQlStep("limit", positionalArgs = listOf(SaneQlInt(limit))))
            }
        }

    companion object {
        /**
         * @param includeSublineagesFor when non-null, names the single lineage-indexed field to group by
         * *including all sublineages*. In this case [groupByFields] is ignored and the group-by column is
         * rendered as `lineage(<field>, includeSublineages:=true)`. Callers must have validated that this
         * field is a lineage field, is present in [groupByFields], and is the only group-by field.
         */
        fun aggregated(
            groupByFields: List<String> = emptyList(),
            orderByFields: OrderBySpec = OrderBySpec.EMPTY,
            limit: Int? = null,
            offset: Int? = null,
            includeSublineagesFor: String? = null,
        ): SiloAction<AggregationData> =
            AggregatedAction(
                groupByFields = when (includeSublineagesFor) {
                    null -> groupByFields.map { GroupByField.Plain(it) }
                    else -> listOf(GroupByField.LineageWithSublineages(includeSublineagesFor))
                },
                orderByFields = getOrderByFieldsList(orderByFields),
                randomize = getRandomize(orderByFields),
                limit = limit,
                offset = offset,
            )

        fun mutations(
            minProportion: Double? = null,
            orderByFields: OrderBySpec = OrderBySpec.EMPTY,
            limit: Int? = null,
            offset: Int? = null,
            fields: List<String> = emptyList(),
        ): SiloAction<MutationData> =
            MutationsAction(
                minProportion = minProportion,
                orderByFields = getOrderByFieldsList(orderByFields),
                randomize = getRandomize(orderByFields),
                limit = limit,
                offset = offset,
                fields = fields,
            )

        fun aminoAcidMutations(
            minProportion: Double? = null,
            orderByFields: OrderBySpec = OrderBySpec.EMPTY,
            limit: Int? = null,
            offset: Int? = null,
            fields: List<String> = emptyList(),
        ): SiloAction<MutationData> =
            AminoAcidMutationsAction(
                minProportion = minProportion,
                orderByFields = getOrderByFieldsList(orderByFields),
                randomize = getRandomize(orderByFields),
                limit = limit,
                offset = offset,
                fields = fields,
            )

        fun details(
            fields: List<String> = emptyList(),
            orderByFields: OrderBySpec = OrderBySpec.EMPTY,
            limit: Int? = null,
            offset: Int? = null,
        ): SiloAction<DetailsData> =
            DetailsAction(
                fields = fields,
                orderByFields = getOrderByFieldsList(orderByFields),
                randomize = getRandomize(orderByFields),
                limit = limit,
                offset = offset,
            )

        fun mostRecentCommonAncestor(
            phyloTreeField: String,
            printNodesNotInTree: Boolean = false,
        ): SiloAction<MostCommonAncestorData> =
            MostRecentCommonAncestorAction(
                columnName = phyloTreeField,
                printNodesNotInTree = printNodesNotInTree,
            )

        fun phyloSubtree(
            phyloTreeField: String,
            printNodesNotInTree: Boolean = false,
        ): SiloAction<PhyloSubtreeData> =
            PhyloSubtreeAction(
                columnName = phyloTreeField,
                printNodesNotInTree = printNodesNotInTree,
            )

        fun nucleotideInsertions(
            orderByFields: OrderBySpec = OrderBySpec.EMPTY,
            limit: Int? = null,
            offset: Int? = null,
        ): SiloAction<InsertionData> =
            NucleotideInsertionsAction(
                orderByFields = getOrderByFieldsList(orderByFields),
                limit = limit,
                offset = offset,
                randomize = getRandomize(orderByFields),
            )

        fun aminoAcidInsertions(
            orderByFields: OrderBySpec = OrderBySpec.EMPTY,
            limit: Int? = null,
            offset: Int? = null,
        ): SiloAction<InsertionData> =
            AminoAcidInsertionsAction(
                orderByFields = getOrderByFieldsList(orderByFields),
                limit = limit,
                offset = offset,
                randomize = getRandomize(orderByFields),
            )

        fun genomicSequence(
            type: SequenceType,
            sequenceNames: List<String>,
            additionalFields: List<String> = emptyList(),
            orderByFields: OrderBySpec = OrderBySpec.EMPTY,
            limit: Int? = null,
            offset: Int? = null,
        ): SiloAction<SequenceData> =
            SequenceAction(
                type = type,
                sequenceNames = sequenceNames,
                additionalFields = additionalFields,
                orderByFields = getOrderByFieldsList(orderByFields),
                limit = limit,
                offset = offset,
                randomize = getRandomize(orderByFields),
            )

        private fun getRandomize(orderByFields: OrderBySpec): RandomizeConfig =
            when (orderByFields) {
                is OrderBySpec.ByFields -> {
                    RandomizeConfig.Disabled
                }

                is OrderBySpec.Random -> {
                    orderByFields.seed?.let { RandomizeConfig.WithSeed(it) }
                        ?: RandomizeConfig.Enabled
                }
            }

        private fun getOrderByFieldsList(orderByFields: OrderBySpec): List<OrderByField> =
            when (orderByFields) {
                is OrderBySpec.ByFields -> orderByFields.fields
                is OrderBySpec.Random -> emptyList()
            }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    data class AggregatedAction(
        val groupByFields: List<GroupByField>,
        override val orderByFields: List<OrderByField> = emptyList(),
        override val randomize: RandomizeConfig? = null,
        override val limit: Int? = null,
        override val offset: Int? = null,
    ) : SiloAction<AggregationData>(
            arrowConverter = AGGREGATION_DATA_ARROW_CONVERTER,
            cacheable = true,
        ) {
        val type: String = "Aggregated"

        override fun ownSaneQlSteps() =
            listOf(
                SaneQlStep(
                    "groupBy",
                    positionalArgs = buildList {
                        add(SaneQlList(listOf(SaneQlAssignment("count", SaneQlFunctionCall("count")))))
                        if (groupByFields.isNotEmpty()) {
                            add(SaneQlList(groupByFields.map { it.toSaneQl() }))
                        }
                    },
                ),
            )
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    data class MutationsAction(
        val minProportion: Double?,
        override val orderByFields: List<OrderByField> = emptyList(),
        override val randomize: RandomizeConfig? = null,
        override val limit: Int? = null,
        override val offset: Int? = null,
        val fields: List<String> = emptyList(),
    ) : SiloAction<MutationData>(
            arrowConverter = MUTATION_DATA_ARROW_CONVERTER,
            cacheable = true,
        ) {
        val type: String = "Mutations"

        override fun ownSaneQlSteps() =
            listOf(SaneQlStep("mutations", namedArgs = mutationsNamedArgs(minProportion, fields)))
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    data class AminoAcidMutationsAction(
        val minProportion: Double?,
        override val orderByFields: List<OrderByField> = emptyList(),
        override val randomize: RandomizeConfig? = null,
        override val limit: Int? = null,
        override val offset: Int? = null,
        val fields: List<String> = emptyList(),
    ) : SiloAction<MutationData>(
            arrowConverter = MUTATION_DATA_ARROW_CONVERTER,
            cacheable = true,
        ) {
        val type: String = "AminoAcidMutations"

        override fun ownSaneQlSteps() =
            listOf(SaneQlStep("aminoAcidMutations", namedArgs = mutationsNamedArgs(minProportion, fields)))
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    data class DetailsAction(
        val fields: List<String> = emptyList(),
        override val orderByFields: List<OrderByField> = emptyList(),
        override val randomize: RandomizeConfig? = null,
        override val limit: Int? = null,
        override val offset: Int? = null,
    ) : SiloAction<DetailsData>(
            arrowConverter = DETAILS_DATA_ARROW_CONVERTER,
            cacheable = false,
        ) {
        val type: String = "Details"

        override fun ownSaneQlSteps() =
            if (fields.isEmpty()) {
                emptyList()
            } else {
                listOf(SaneQlStep("project", positionalArgs = listOf(SaneQlList(fields.map { id(it) }))))
            }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    data class NucleotideInsertionsAction(
        override val orderByFields: List<OrderByField> = emptyList(),
        override val randomize: RandomizeConfig? = null,
        override val limit: Int? = null,
        override val offset: Int? = null,
    ) : SiloAction<InsertionData>(
            arrowConverter = INSERTION_DATA_ARROW_CONVERTER,
            cacheable = true,
        ) {
        val type: String = "Insertions"

        override fun ownSaneQlSteps() = listOf(SaneQlStep("insertions"))
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    data class MostRecentCommonAncestorAction(
        val columnName: String,
        val printNodesNotInTree: Boolean? = false,
        override val orderByFields: List<OrderByField> = emptyList(),
        override val limit: Int? = null,
        override val offset: Int? = null,
        override val randomize: RandomizeConfig? = null,
    ) : SiloAction<MostCommonAncestorData>(
            arrowConverter = MOST_COMMON_ANCESTOR_DATA_ARROW_CONVERTER,
            cacheable = true,
        ) {
        val type: String = "MostRecentCommonAncestor"

        override fun ownSaneQlSteps() =
            listOf(
                SaneQlStep(
                    "mostRecentCommonAncestor",
                    positionalArgs = listOf(str(columnName)),
                    namedArgs = printNodesNotInTreeNamedArgs(printNodesNotInTree),
                ),
            )
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    data class PhyloSubtreeAction(
        val columnName: String,
        val printNodesNotInTree: Boolean? = false,
        override val orderByFields: List<OrderByField> = emptyList(),
        override val limit: Int? = null,
        override val offset: Int? = null,
        override val randomize: RandomizeConfig? = null,
    ) : SiloAction<PhyloSubtreeData>(
            arrowConverter = PHYLO_SUBTREE_DATA_ARROW_CONVERTER,
            cacheable = true,
        ) {
        val type: String = "PhyloSubtree"

        override fun ownSaneQlSteps() =
            listOf(
                SaneQlStep(
                    "phyloSubtree",
                    positionalArgs = listOf(str(columnName)),
                    namedArgs = printNodesNotInTreeNamedArgs(printNodesNotInTree),
                ),
            )
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    data class AminoAcidInsertionsAction(
        override val orderByFields: List<OrderByField> = emptyList(),
        override val randomize: RandomizeConfig? = null,
        override val limit: Int? = null,
        override val offset: Int? = null,
    ) : SiloAction<InsertionData>(
            arrowConverter = INSERTION_DATA_ARROW_CONVERTER,
            cacheable = true,
        ) {
        val type: String = "AminoAcidInsertions"

        override fun ownSaneQlSteps() = listOf(SaneQlStep("aminoAcidInsertions"))
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    data class SequenceAction(
        override val orderByFields: List<OrderByField> = emptyList(),
        override val randomize: RandomizeConfig? = null,
        override val limit: Int? = null,
        override val offset: Int? = null,
        val type: SequenceType,
        val sequenceNames: List<String>,
        val additionalFields: List<String> = emptyList(),
    ) : SiloAction<SequenceData>(
            arrowConverter = SEQUENCE_DATA_ARROW_CONVERTER,
            cacheable = false,
        ) {
        override fun ownSaneQlSteps(): List<SaneQlStep> {
            val allFields = additionalFields + sequenceNames
            return listOf(SaneQlStep("project", positionalArgs = listOf(SaneQlList(allFields.map { id(it) }))))
        }
    }
}

/**
 * A single group-by column of an [SiloAction.AggregatedAction].
 *
 * [Plain] renders as a bare column identifier (`id(name)` → `"name"`), exactly as before.
 * [LineageWithSublineages] renders as `lineage("name", includeSublineages:=true)`, telling SILO to
 * produce one count per defined lineage where each count includes that lineage's sublineages.
 */
@JsonSerialize(using = GroupByFieldSerializer::class)
sealed interface GroupByField {
    val name: String

    fun toSaneQl(): SaneQlExpression

    data class Plain(
        override val name: String,
    ) : GroupByField {
        override fun toSaneQl(): SaneQlExpression = id(name)
    }

    data class LineageWithSublineages(
        override val name: String,
    ) : GroupByField {
        override fun toSaneQl(): SaneQlExpression =
            SaneQlFunctionCall(
                "lineage",
                positionalArgs = listOf(id(name)),
                namedArgs = listOf(SaneQlNamedArg("includeSublineages", SaneQlBoolean(true))),
            )
    }
}

/**
 * Serializes [GroupByField] so the (non-wire, informational) action JSON keeps its historic shape:
 * a [GroupByField.Plain] becomes a bare string, so `groupByFields` still renders as `["a", "b"]`.
 * A [GroupByField.LineageWithSublineages] becomes `{"lineage": "...", "includeSublineages": true}`.
 * The actual query sent to SILO is the SaneQL string from [SiloQuery.toSaneQl], not this JSON.
 */
class GroupByFieldSerializer : ValueSerializer<GroupByField>() {
    override fun serialize(
        value: GroupByField,
        gen: JsonGenerator,
        serializers: SerializationContext,
    ) {
        when (value) {
            is GroupByField.Plain -> gen.writeString(value.name)
            is GroupByField.LineageWithSublineages -> {
                gen.writeStartObject()
                gen.writeStringProperty("lineage", value.name)
                gen.writeBooleanProperty("includeSublineages", true)
                gen.writeEndObject()
            }
        }
    }
}

private fun mutationsNamedArgs(
    minProportion: Double?,
    fields: List<String>,
): List<SaneQlNamedArg> =
    buildList {
        if (minProportion != null) {
            add(SaneQlNamedArg("minProportion", SaneQlFloat(minProportion)))
        }
        if (fields.isNotEmpty()) {
            add(SaneQlNamedArg("fields", SaneQlList(fields.map { id(it) })))
        }
    }

private fun printNodesNotInTreeNamedArgs(printNodesNotInTree: Boolean?): List<SaneQlNamedArg> =
    if (printNodesNotInTree == true) {
        listOf(SaneQlNamedArg("printNodesNotInTree", SaneQlBoolean(true)))
    } else {
        emptyList()
    }

private fun toSaneQl(field: OrderByField): SaneQlExpression =
    when (field.order) {
        Order.ASCENDING -> id(field.field)
        Order.DESCENDING -> SaneQlMethodCall(id(field.field), "desc")
    }

sealed class SiloFilterExpression(
    val type: String,
) {
    /** Renders this filter expression as a SaneQL expression, e.g. `"theColumn" = 'theValue'`. */
    abstract fun toSaneQl(): SaneQlExpression
}

data class StringEquals(
    val column: String,
    val value: String?,
) : SiloFilterExpression("StringEquals") {
    override fun toSaneQl() = if (value == null) isNull(column) else SaneQlEquals(id(column), str(value))
}

data class BooleanEquals(
    val column: String,
    val value: Boolean?,
) : SiloFilterExpression("BooleanEquals") {
    override fun toSaneQl() = if (value == null) isNull(column) else SaneQlEquals(id(column), SaneQlBoolean(value))
}

data class LineageEquals(
    val column: String,
    val value: String?,
    val includeSublineages: Boolean,
) : SiloFilterExpression("Lineage") {
    override fun toSaneQl() =
        SaneQlMethodCall(
            receiver = id(column),
            name = "lineage",
            positionalArgs = listOf(if (value == null) SaneQlNull else str(value)),
            namedArgs = listOf(SaneQlNamedArg("includeSublineages", SaneQlBoolean(includeSublineages))),
        )
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NucleotideSymbolEquals(
    val sequenceName: String?,
    val position: Int,
    val symbol: String,
) : SiloFilterExpression("NucleotideEquals") {
    override fun toSaneQl() =
        SaneQlFunctionCall(
            "nucleotideEquals",
            namedArgs = buildList {
                add(SaneQlNamedArg("position", SaneQlInt(position)))
                add(SaneQlNamedArg("symbol", str(symbol)))
                if (sequenceName != null) {
                    add(SaneQlNamedArg("sequenceName", str(sequenceName)))
                }
            },
        )
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class HasNucleotideMutation(
    val sequenceName: String?,
    val position: Int,
) : SiloFilterExpression("HasNucleotideMutation") {
    override fun toSaneQl() =
        SaneQlFunctionCall(
            "hasMutation",
            namedArgs = buildList {
                add(SaneQlNamedArg("position", SaneQlInt(position)))
                if (sequenceName != null) {
                    add(SaneQlNamedArg("sequenceName", str(sequenceName)))
                }
            },
        )
}

data class AminoAcidSymbolEquals(
    val sequenceName: String,
    val position: Int,
    val symbol: String,
) : SiloFilterExpression("AminoAcidEquals") {
    override fun toSaneQl() =
        SaneQlFunctionCall(
            "aminoAcidEquals",
            namedArgs = listOf(
                SaneQlNamedArg("position", SaneQlInt(position)),
                SaneQlNamedArg("symbol", str(symbol)),
                SaneQlNamedArg("sequenceName", str(sequenceName)),
            ),
        )
}

data class HasAminoAcidMutation(
    val sequenceName: String,
    val position: Int,
) : SiloFilterExpression("HasAminoAcidMutation") {
    override fun toSaneQl() =
        SaneQlFunctionCall(
            "hasAAMutation",
            namedArgs = listOf(
                SaneQlNamedArg("position", SaneQlInt(position)),
                SaneQlNamedArg("sequenceName", str(sequenceName)),
            ),
        )
}

data class DateBetween(
    val column: String,
    val from: LocalDate?,
    val to: LocalDate?,
) : SiloFilterExpression("DateBetween") {
    override fun toSaneQl() =
        SaneQlMethodCall(
            receiver = id(column),
            name = "between",
            positionalArgs = listOf(SaneQlDate(from), SaneQlDate(to)),
        )
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NucleotideInsertionContains(
    val position: Int,
    val value: String,
    val sequenceName: String?,
) : SiloFilterExpression("InsertionContains") {
    override fun toSaneQl() =
        SaneQlFunctionCall(
            "insertionContains",
            namedArgs = buildList {
                add(SaneQlNamedArg("position", SaneQlInt(position)))
                add(SaneQlNamedArg("value", str(value)))
                if (sequenceName != null) {
                    add(SaneQlNamedArg("sequenceName", str(sequenceName)))
                }
            },
        )
}

data class AminoAcidInsertionContains(
    val position: Int,
    val value: String,
    val sequenceName: String,
) : SiloFilterExpression(
        "AminoAcidInsertionContains",
    ) {
    override fun toSaneQl() =
        SaneQlFunctionCall(
            "aminoAcidInsertionContains",
            namedArgs = listOf(
                SaneQlNamedArg("position", SaneQlInt(position)),
                SaneQlNamedArg("value", str(value)),
                SaneQlNamedArg("sequenceName", str(sequenceName)),
            ),
        )
}

data object True : SiloFilterExpression("True") {
    override fun toSaneQl() = SaneQlBoolean(true)
}

data class And(
    val children: List<SiloFilterExpression>,
) : SiloFilterExpression("And") {
    constructor(vararg children: SiloFilterExpression) : this(children.toList())

    override fun toSaneQl() = SaneQlAnd(children.map { it.toSaneQl() })
}

data class Or(
    val children: List<SiloFilterExpression>,
) : SiloFilterExpression("Or") {
    constructor(vararg children: SiloFilterExpression) : this(children.toList())

    override fun toSaneQl() = SaneQlOr(children.map { it.toSaneQl() })
}

data class Not(
    val child: SiloFilterExpression,
) : SiloFilterExpression("Not") {
    override fun toSaneQl() = SaneQlNot(child.toSaneQl())
}

data class Maybe(
    val child: SiloFilterExpression,
) : SiloFilterExpression("Maybe") {
    override fun toSaneQl() = SaneQlFunctionCall("maybe", positionalArgs = listOf(child.toSaneQl()))
}

data class NOf(
    val numberOfMatchers: Int,
    val matchExactly: Boolean,
    val children: List<SiloFilterExpression>,
) : SiloFilterExpression("N-Of") {
    override fun toSaneQl() =
        SaneQlFunctionCall(
            "nOf",
            positionalArgs = listOf(SaneQlInt(numberOfMatchers), SaneQlList(children.map { it.toSaneQl() })),
            namedArgs = if (matchExactly) {
                listOf(SaneQlNamedArg("matchExactly", SaneQlBoolean(true)))
            } else {
                emptyList()
            },
        )
}

data class IntEquals(
    val column: String,
    val value: Int?,
) : SiloFilterExpression("IntEquals") {
    override fun toSaneQl() = if (value == null) isNull(column) else SaneQlEquals(id(column), SaneQlInt(value))
}

data class IntBetween(
    val column: String,
    val from: Int?,
    val to: Int?,
) : SiloFilterExpression("IntBetween") {
    override fun toSaneQl() =
        SaneQlMethodCall(
            receiver = id(column),
            name = "between",
            positionalArgs = listOf(intOrNull(from), intOrNull(to)),
        )
}

data class FloatEquals(
    val column: String,
    val value: Double?,
) : SiloFilterExpression("FloatEquals") {
    override fun toSaneQl() = if (value == null) isNull(column) else SaneQlEquals(id(column), SaneQlFloat(value))
}

data class FloatBetween(
    val column: String,
    val from: Double?,
    val to: Double?,
) : SiloFilterExpression("FloatBetween") {
    override fun toSaneQl() =
        SaneQlMethodCall(
            receiver = id(column),
            name = "between",
            positionalArgs = listOf(floatOrNull(from), floatOrNull(to)),
        )
}

data class StringSearch(
    val column: String,
    val searchExpression: String,
) : SiloFilterExpression("StringSearch") {
    override fun toSaneQl() =
        SaneQlMethodCall(receiver = id(column), name = "like", positionalArgs = listOf(str(searchExpression)))
}

data class PhyloDescendantOf(
    val column: String,
    val internalNode: String,
) : SiloFilterExpression("PhyloDescendantOf") {
    override fun toSaneQl() =
        SaneQlMethodCall(receiver = id(column), name = "phyloDescendantOf", positionalArgs = listOf(str(internalNode)))
}

data class IsNull(
    val column: String,
) : SiloFilterExpression("IsNull") {
    override fun toSaneQl() = isNull(column)
}

data class IsNotNull(
    val column: String,
) : SiloFilterExpression("IsNotNull") {
    override fun toSaneQl() = SaneQlFunctionCall("isNotNull", positionalArgs = listOf(id(column)))
}

private fun id(name: String) = SaneQlIdentifier(name)

private fun str(value: String) = SaneQlString(value)

private fun isNull(column: String) = SaneQlFunctionCall("isNull", positionalArgs = listOf(id(column)))

private fun intOrNull(value: Int?): SaneQlExpression = if (value == null) SaneQlNull else SaneQlInt(value)

private fun floatOrNull(value: Double?): SaneQlExpression = if (value == null) SaneQlNull else SaneQlFloat(value)

enum class SequenceType {
    @JsonProperty("Fasta")
    UNALIGNED,

    @JsonProperty("FastaAligned")
    ALIGNED,
}

@JsonSerialize(using = RandomizeConfigSerializer::class)
sealed class RandomizeConfig {
    data object Enabled : RandomizeConfig()

    data object Disabled : RandomizeConfig()

    data class WithSeed(
        val seed: Int,
    ) : RandomizeConfig()
}

class RandomizeConfigSerializer : ValueSerializer<RandomizeConfig>() {
    override fun serialize(
        value: RandomizeConfig,
        gen: JsonGenerator,
        serializers: SerializationContext,
    ) {
        when (value) {
            is RandomizeConfig.Enabled -> {
                gen.writeBoolean(true)
            }

            is RandomizeConfig.Disabled -> {
                gen.writeBoolean(false)
            }

            is RandomizeConfig.WithSeed -> {
                gen.writeStartObject()
                gen.writeNumberProperty("seed", value.seed)
                gen.writeEndObject()
            }
        }
    }
}
