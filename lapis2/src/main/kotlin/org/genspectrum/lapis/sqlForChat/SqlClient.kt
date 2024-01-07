package org.genspectrum.lapis.sqlForChat

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import net.sf.jsqlparser.JSQLParserException
import net.sf.jsqlparser.expression.*
import net.sf.jsqlparser.expression.Function
import net.sf.jsqlparser.expression.operators.conditional.AndExpression
import net.sf.jsqlparser.expression.operators.conditional.OrExpression
import net.sf.jsqlparser.expression.operators.relational.*
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.schema.Column
import net.sf.jsqlparser.schema.Table
import net.sf.jsqlparser.statement.select.*
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.DatabaseMetadata
import org.genspectrum.lapis.config.MetadataType
import org.genspectrum.lapis.model.NEXTCLADE_PANGO_LINEAGE_COLUMN
import org.genspectrum.lapis.response.COUNT_PROPERTY
import org.genspectrum.lapis.response.MutationData
import org.genspectrum.lapis.silo.*
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.*
import java.util.function.Predicate

@Service
class SqlClient(
    private val databaseConfig: DatabaseConfig,
    private val siloClient: SiloClient,
    private val objectMapper: ObjectMapper,
) {

    /**
     * Parses a SQL and checks that only supported SQL constructs are used. There is one limitation: it does not check
     * the content of the WHERE and HAVING clauses. This shall be done by the validate() function.
     */
    fun parse(sql: String?): Query {
        try {
            val query = Query()

            // Check that it is a plain SELECT statement with not more than following top-level keywords:
            // select, from, where, group by, having, order by, limit, offset
            val statement = CCJSqlParserUtil.parse(sql)
            if (statement !is Select) throw UnsupportedSqlException()
            if (statement.withItemsList != null || statement.isUsingWithBrackets) {
                throw UnsupportedSqlException()
            }
            val selectBody: SelectBody = statement.selectBody
            if (selectBody !is PlainSelect) throw UnsupportedSqlException()
            if ( // The distinct keyword shall be ignored for now. It shouldn't be a problem in most cases as we
            // copy over the values in SELECT into GROUP BY
            //plainSelect.getDistinct() != null ||
                selectBody.intoTables != null ||
                selectBody.joins != null ||
                selectBody.fetch != null ||
                selectBody.optimizeFor != null ||
                selectBody.skip != null ||
                selectBody.mySqlHintStraightJoin ||
                selectBody.top != null ||
                selectBody.oracleHierarchical != null ||
                selectBody.oracleHint != null ||
                selectBody.isOracleSiblings ||
                selectBody.isForUpdate ||
                selectBody.forUpdateTable != null ||
                selectBody.isSkipLocked ||
                selectBody.isUseBrackets ||
                selectBody.wait != null ||
                selectBody.mySqlSqlCalcFoundRows ||
                selectBody.mySqlSqlCacheFlag != null ||
                selectBody.forXmlPath != null ||
                selectBody.ksqlWindow != null ||
                selectBody.isNoWait ||
                selectBody.isEmitChanges ||
                selectBody.withIsolation != null ||
                selectBody.windowDefinitions != null
            ) {
                throw UnsupportedSqlException()
            }

            // SELECT
            val selectItems = selectBody.selectItems
            val selectExpressionsAndAliases = extractSelectExpressionsAndAliases(selectItems)
            query.aliasToExpression = selectExpressionsAndAliases.first
            query.selectExpressions = selectExpressionsAndAliases.second

            // FROM
            // Only accept a single table
            val fromItem = selectBody.fromItem
            if (fromItem !is Table) throw UnsupportedSqlException()
            query.table = fromItem.toString()

            // WHERE
            // No further validation and processing at the moment
            query.whereExpression = selectBody.where

            // GROUP BY
            val groupBy = selectBody.groupBy
            if (groupBy != null) {
                val expressionList = groupBy.groupByExpressionList
                val expressions = expressionList.expressions
                for (expression in expressions) {
                    if (expression !is Column) throw UnsupportedSqlException()
                    query.groupByColumns.addLast(expression.columnName)
                }
            }

            // HAVING
            // No further validation and processing at the moment
            query.havingExpression = selectBody.having

            // ORDER BY
            val orderByElements = selectBody.orderByElements
            if (orderByElements != null && orderByElements.isNotEmpty()) {
                // Don't want more than 1 expression at the moment because it gets to complicated...
                if (orderByElements.size > 1) throw UnsupportedSqlException()
                val orderByElement = orderByElements[0]
                val expression = orderByElement.expression
                if (expression is Column && expression.table == null) {
                    query.orderByExpression = expression.columnName
                } else if (expression is Function) {
                    query.orderByExpression = expression.toString()
                } else {
                    throw UnsupportedSqlException()
                }
                query.orderByAsc = orderByElement.isAsc
            }

            // OFFSET
            val offset = selectBody.offset
            if (offset != null) {
                if (offset.offset !is LongValue || offset.offsetParam != null) {
                    throw UnsupportedSqlException()
                }
                query.offset = (offset.offset as LongValue).value.toInt()
            }

            // LIMIT
            val limit = selectBody.limit
            if (limit != null) {
                val rowCount = limit.rowCount
                if (rowCount != null) {
                    if (rowCount !is LongValue) throw UnsupportedSqlException()
                    query.limit = rowCount.value.toInt()
                }
                // LIMIT with offset
                val limitOffsetExpression = limit.offset
                if (limit.offset != null) {
                    if (limitOffsetExpression !is LongValue) throw UnsupportedSqlException()
                    val offsetInteger = limitOffsetExpression.value.toInt()
                    if (query.offset != null && query.offset != offsetInteger) {
                        // TODO Why should someone provide offset twice and with different values? It's probably just
                        //  wrong. But maybe it has a semantic that I am not aware of?
                        throw UnsupportedSqlException()
                    }
                    query.offset = offsetInteger
                }
            }

            return query
        } catch (e: JSQLParserException) {
            throw RuntimeException(e)
        }
    }

    /**
     *
     *  1. Check the fields (existence and compatibility)
     *  1. Rewrite: Use "nextcladePangoLineage"
     *  1. Rewrite: resolve aliases in ORDER BY (but not in HAVING)
     *
     */
    fun validateAndRewrite(query: Query) {
        // Table
        val table = query.table
        if (table != "metadata" && table != "aa_mutations" && table != "nuc_mutations") {
            throw UnsupportedSqlException()
        }

        // SELECT, GROUP BY, and ORDER BY
        if (table == "metadata") {
            validateAndRewriteMetadataQuery(query)
        } else {
            validateAndRewriteMutationQuery(query)
        }

        // WHERE
        val where = query.whereExpression
        if (where != null) {
            query.whereQueryExpr = validateAndRewriteWhereExpression(where)
        }
    }

    fun executeToJson(query: Query): String {
        return when (query.table) {
            "metadata" -> executeMetadataQuery(query)
            "nuc_mutations" -> executeMutationsQuery(query, true)
            "aa_mutations" -> executeMutationsQuery(query, false)
            else -> throw RuntimeException("Unexpected error")
        }
    }

    private fun extractSelectExpressionsAndAliases(selectItems: List<SelectItem>): Pair<MutableMap<String, String>, MutableList<String>> {
        val aliasToExpression = mutableMapOf<String, String>()
        val selectExpressions = mutableListOf<String>()
        for (selectItem in selectItems) {
            if (selectItem !is SelectExpressionItem) throw UnsupportedSqlException()
            val expression: Expression = selectItem.expression
            val selectExpression = if (expression is Column && expression.table == null) {
                expression.columnName
            } else if (expression is Function) {
                expression.toString()
            } else {
                throw UnsupportedSqlException()
            }
            if (selectItem.alias != null && selectItem.alias.name != null) {
                aliasToExpression[selectItem.alias.name] = selectExpression
            }
            selectExpressions.add(selectExpression)
        }
        return Pair(aliasToExpression, selectExpressions)
    }

    private fun validateAndRewriteMetadataQuery(query: Query) {
        // SELECT: only allow metadata fields and count(*)
        val newSelectExpressions: MutableList<String> = ArrayList()
        for (selectExpression in query.selectExpressions) {
            val metadataField = validateAndRewriteMetadataField(selectExpression)
            if (metadataField != null) {
                newSelectExpressions.add(metadataField.name)
            } else if (selectExpression == "count(*)") {
                newSelectExpressions.add(selectExpression)
            } else if (selectExpression == "date_trunc('year', date)") {
                newSelectExpressions.add("year")
            } else if (selectExpression == "date_trunc('month', date)") {
                newSelectExpressions.add("year")
                newSelectExpressions.add("month")
            } else {
                throw UnsupportedSqlException()
            }
        }
        query.selectExpressions = newSelectExpressions

        // GROUP BY: only allow metadata fields
        var oldGroupByColumns = query.groupByColumns
        // This is now a very "opinionated" rewrite: because we do not allow querying of individual samples,
        // we can't evaluate something like "select date from metadata order by date limit 1". As a hack,
        // such a query will be rewritten to "select date from metadata group by date order by date limit 1".
        // More generally, we will add all metadata columns occurring in SELECT to GROUP BY.
        // This might return something different from what the user wants but hopefully, it's in most cases not to far.
        oldGroupByColumns.addAll(newSelectExpressions.stream().filter { e: String -> e != "count(*)" }.toList())
        oldGroupByColumns = ArrayList(HashSet(oldGroupByColumns))
        val newGroupByColumns: MutableList<String> = ArrayList()
        for (groupByColumn in oldGroupByColumns) {
            val metadataField = validateAndRewriteMetadataField(groupByColumn)
            if (metadataField != null) {
                newGroupByColumns.add(metadataField.name)
            } else if (query.aliasToExpression.getOrDefault(groupByColumn, groupByColumn).startsWith("date_trunc(")) {
                // That's fine. date_trunc() was already resolved above.
            } else {
                throw UnsupportedSqlException()
            }
        }
        query.groupByColumns = newGroupByColumns

        // HAVING
        // Only a simple comparison is supported with count(*) on the left and constant on the right
        val having = query.havingExpression
        if (having != null) {
            if (having !is ComparisonOperator) throw UnsupportedSqlException()
            var leftName: String = having.leftExpression.toString()
            val right: Expression = having.rightExpression
            if (query.aliasToExpression.containsKey(leftName)) {
                leftName = query.aliasToExpression.get(leftName)!!
            }
            if (leftName != "count(*)") throw UnsupportedSqlException()
            if (!(right is LongValue || right is DoubleValue)) throw UnsupportedSqlException()
            if (having is GeometryDistance) throw UnsupportedSqlException() // Other comparisons are OK
        }

        // ORDER BY: it must be a metadata field mentioned in GROUP BY or count(*)
        var orderBy = query.orderByExpression
        if (orderBy != null) {
            // If an alias is used, it will be resolved and replaced.
            if (query.aliasToExpression.containsKey(orderBy)) {
                orderBy = query.aliasToExpression.get(orderBy)!!
                query.orderByExpression = orderBy
            }
            val metadataField = validateAndRewriteMetadataField(orderBy)
            if (metadataField != null) {
                if (!query.groupByColumns.contains(metadataField.name)) throw UnsupportedSqlException()
            } else if (orderBy != "count(*)") {
                throw UnsupportedSqlException()
            }
        }
    }

    private fun validateAndRewriteMutationQuery(query: Query) {
        // SELECT: only allow "mutation", count(*), and proportion()
        for (selectExpression in query.selectExpressions) {
            if (selectExpression != "mutation" && selectExpression != "count(*)" &&
                selectExpression != "proportion()"
            ) {
                throw UnsupportedSqlException()
            }
        }

        // GROUP BY: it must be grouped by "mutation" and only by "mutation.
        val groupByColumns = query.groupByColumns
        if (groupByColumns.size != 1 || groupByColumns[0] != "mutation") {
            throw UnsupportedSqlException()
        }

        // HAVING
        // Only a simple comparison is supported with count(*) or proportion() on the left and constant on the right
        val having = query.havingExpression
        if (having != null) {
            if (having !is ComparisonOperator) throw UnsupportedSqlException()
            var leftName = having.leftExpression.toString()
            val right = having.rightExpression
            if (query.aliasToExpression.containsKey(leftName)) {
                leftName = query.aliasToExpression.get(leftName)!!
            }
            if (leftName != "count(*)" && leftName != "proportion()") throw UnsupportedSqlException()
            if (!(right is LongValue || right is DoubleValue)) throw UnsupportedSqlException()
            if (having is GeometryDistance) throw UnsupportedSqlException() // Other comparisons are OK
        }

        // ORDER BY: it must be "mutation", count(*), or proportion()
        var orderBy = query.orderByExpression
        if (orderBy != null) {
            // If an alias is used, it will be resolved and replaced.
            if (query.aliasToExpression.containsKey(orderBy)) {
                orderBy = query.aliasToExpression.get(orderBy)
                query.orderByExpression = orderBy
            }
            if (!(orderBy == "mutation" || orderBy == "count(*)" || orderBy == "proportion()")) {
                throw UnsupportedSqlException()
            }
        }
    }

    private fun validateAndRewriteWhereExpression(expression: Expression): SiloFilterExpression {
        if (expression is Parenthesis) {
            return validateAndRewriteWhereExpression(expression.expression)
        } else if (expression is AndExpression) {
            val leftExpr = validateAndRewriteWhereExpression(expression.leftExpression)
            val rightExpr = validateAndRewriteWhereExpression(expression.rightExpression)
            return And(listOf(leftExpr, rightExpr))
        } else if (expression is OrExpression) {
            val leftExpr = validateAndRewriteWhereExpression(expression.leftExpression)
            val rightExpr = validateAndRewriteWhereExpression(expression.rightExpression)
            return Or(listOf(leftExpr, rightExpr))
        } else if (expression is NotExpression) {
            val expr = validateAndRewriteWhereExpression(expression.expression)
            return Not(expr)
        } else if (expression is ComparisonOperator) {
            val left: Expression = expression.leftExpression
            val right: Expression = expression.rightExpression

            // The left side must be a column
            if (left !is Column) throw UnsupportedSqlException()
            val columnName: String = left.columnName

            // It can be a metadata field or a mutation column encoded as nuc_123 or aa_S_234
            val metadataColumn = validateAndRewriteMetadataField(columnName)
            if (metadataColumn != null) {
                left.columnName = metadataColumn.name
                // The right side must be a constant value. The type of the value must fit the column.
                // The operator must also make sense.
                val columnType = metadataColumn.type
                when (columnType) {
                    org.genspectrum.lapis.config.MetadataType.STRING, org.genspectrum.lapis.config.MetadataType.PANGO_LINEAGE -> {
                        if (right !is StringValue) throw UnsupportedSqlException()
                        val expr = if (metadataColumn.name == NEXTCLADE_PANGO_LINEAGE_COLUMN) {
                            PangoLineageEquals(NEXTCLADE_PANGO_LINEAGE_COLUMN, right.value, true)
                        } else {
                            StringEquals(metadataColumn.name, right.value)
                        }
                        return when (expression) {
                            is EqualsTo -> expr
                            is NotEqualsTo -> Not(expr)
                            else -> throw UnsupportedSqlException()
                        }
                    }

                    org.genspectrum.lapis.config.MetadataType.DATE -> {
                        if (right !is StringValue) throw UnsupportedSqlException()
                        val date = try {
                            LocalDate.parse(right.value)
                        } catch (e: DateTimeParseException) {
                            throw UnsupportedSqlException()
                        }
                        return when (expression) {
                            is EqualsTo -> DateBetween(metadataColumn.name, date, date)
                            is NotEqualsTo -> Not(DateBetween(metadataColumn.name, date, date))
                            is GreaterThan -> And(
                                listOf(
                                    DateBetween(metadataColumn.name, date, null),
                                    Not(DateBetween(metadataColumn.name, date, date)),
                                ),
                            )

                            is GreaterThanEquals -> DateBetween(metadataColumn.name, date, null)
                            is MinorThan -> And(
                                listOf(
                                    DateBetween(metadataColumn.name, null, date),
                                    Not(DateBetween(metadataColumn.name, date, date)),
                                ),
                            )

                            is MinorThanEquals -> DateBetween(metadataColumn.name, null, date)
                            else -> throw UnsupportedSqlException()
                        }
                    }

                    else -> throw UnsupportedSqlException() // Due to laziness
                }
            } else if (columnName.startsWith("nuc_")) {
                val parts = columnName.split("_")
                if (parts.size != 2) throw UnsupportedSqlException()
                val positionStr = parts[1]
                val position = try {
                    Integer.parseUnsignedInt(positionStr)
                } catch (e: NumberFormatException) {
                    throw UnsupportedSqlException()
                }

                // The right side must be a constant string value
                if (right !is StringValue) throw UnsupportedSqlException()
                val value2 = right.value
                if (value2.length != 1) throw UnsupportedSqlException()
                val value3 = value2.uppercase()

                val nucMutation = NucleotideSymbolEquals(null, position, value3)
                return when (expression) {
                    is EqualsTo -> nucMutation
                    is NotEqualsTo -> Not(nucMutation)
                    else -> throw UnsupportedSqlException()
                }
            } else if (columnName.startsWith("aa_")) {
                val parts = columnName.split("_")
                if (parts.size != 3) throw UnsupportedSqlException()
                val gene = parts[1]
                val positionStr = parts[2]
                val position = try {
                    Integer.parseUnsignedInt(positionStr)
                } catch (e: NumberFormatException) {
                    throw UnsupportedSqlException()
                }

                // The right side must be a constant string value
                if (right !is StringValue) throw UnsupportedSqlException()
                val value2: String = right.value
                if (value2.length != 1) throw UnsupportedSqlException()
                val value3 = value2.uppercase()

                val aaMutation = AminoAcidSymbolEquals(gene, position, value3)
                return when (expression) {
                    is EqualsTo -> aaMutation
                    is NotEqualsTo -> Not(aaMutation)
                    else -> throw UnsupportedSqlException()
                }
            } else {
                throw UnsupportedSqlException()
            }
        } else if (expression is Between) {
            val left: Expression = expression.leftExpression
            val rightStart: Expression = expression.betweenExpressionStart
            val rightEnd: Expression = expression.betweenExpressionEnd

            // The left side must be a column
            if (left !is Column) throw UnsupportedSqlException()
            val columnName: String = left.columnName

            // We only support dates (integer and float are not supported due to laziness)
            val columnType = databaseConfig.schema.metadata.find { it.name == columnName }?.type
            if (columnType == null || columnType != MetadataType.DATE) throw UnsupportedSqlException()

            // The values on the right side must be constants and valid dates.
            if (rightStart !is StringValue || rightEnd !is StringValue) throw UnsupportedSqlException()
            try {
                val startDate = LocalDate.parse(rightStart.value)
                val endDate = LocalDate.parse(rightEnd.value)
                return DateBetween(columnName, startDate, endDate)
            } catch (e: DateTimeParseException) {
                throw UnsupportedSqlException()
            }
        } else {
            throw UnsupportedSqlException()
        }
    }

    /**
     * Returns a valid metadata or null
     */
    private fun validateAndRewriteMetadataField(field: String): DatabaseMetadata? {
        if (field.lowercase(Locale.getDefault()).contains("lineage")) {
            return databaseConfig.schema.metadata.find { it.name == NEXTCLADE_PANGO_LINEAGE_COLUMN }
        }
        val fieldMetadata = databaseConfig.schema.metadata.find { it.name == field }
        if (fieldMetadata != null) {
            return fieldMetadata
        }
        return null
    }

    private fun executeMetadataQuery(query: Query): String {
        val throwUnexpected = fun(): Nothing {
            throw RuntimeException("Unexpected error: The validate function should have checked that already.")
        }

        // GROUP BY -> aggregate
        val siloQuery = SiloQuery(
            SiloAction.aggregated(query.groupByColumns),
            query.whereQueryExpr ?: True,
        )
        var result = this.siloClient.sendQuery(siloQuery).map { it.getHeader().zip(it.asArray()).toMap() }

        // HAVING -> another round of filtering
        val having = query.havingExpression as ComparisonOperator?
        if (having != null) {
            var leftName = having.leftExpression.toString()
            val right = having.rightExpression
            if (query.aliasToExpression.containsKey(leftName)) {
                leftName = query.aliasToExpression.get(leftName)!!
            }
            if (leftName != "count(*)") {
                throwUnexpected()
            }
            val value = when (right) {
                is LongValue -> right.value.toDouble()
                is DoubleValue -> right.value
                else -> throwUnexpected()
            }

            // Now, actually start filtering
            result = result.filter {
                when (having) {
                    is EqualsTo -> it[COUNT_PROPERTY]!!.toDouble() == value
                    is NotEqualsTo -> it[COUNT_PROPERTY]!!.toDouble() != value
                    is GreaterThan -> it[COUNT_PROPERTY]!!.toDouble() > value
                    is GreaterThanEquals -> it[COUNT_PROPERTY]!!.toDouble() >= value
                    is MinorThan -> it[COUNT_PROPERTY]!!.toDouble() < value
                    is MinorThanEquals -> it[COUNT_PROPERTY]!!.toDouble() <= value
                    else -> throwUnexpected()
                }
            }
        }

        // ORDER BY
        val orderBy = query.orderByExpression
        if (orderBy != null) {
            var comparator: Comparator<Map<String, String>>
            if (orderBy == "count(*)") {
                comparator = Comparator.comparingInt { it[COUNT_PROPERTY]!!.toInt() }
            } else {
                val orderByMetadata = databaseConfig.schema.metadata.find { it.name == orderBy }
                if (orderByMetadata == null) {
                    throwUnexpected()
                }
                comparator = when (orderByMetadata.type) {
                    MetadataType.STRING, MetadataType.PANGO_LINEAGE -> Comparator.comparing(
                        { it[orderBy] },
                        Comparator.nullsLast(String::compareTo),
                    )

                    MetadataType.DATE -> Comparator.comparing(
                        {
                            if (it[orderBy] != null) {
                                LocalDate.parse(it[orderBy])
                            } else {
                                null
                            }
                        },
                        Comparator.nullsLast(LocalDate::compareTo),
                    )

                    MetadataType.INT -> Comparator.comparing(
                        { it[orderBy]?.toInt() },
                        Comparator.nullsLast(Int::compareTo),
                    )

                    MetadataType.FLOAT -> Comparator.comparing(
                        { it[orderBy]?.toDouble() },
                        Comparator.nullsLast(Double::compareTo),
                    )

                    else -> throwUnexpected()
                }
            }
            if (!query.orderByAsc) {
                comparator = comparator.reversed()
            }
            result = result.sortedWith(comparator)
        }

        // OFFSET and LIMIT
        result = limitAndOffset(result, query)

        return objectMapper.writeValueAsString(result)
    }

    private fun executeMutationsQuery(query: Query, isNucleotide: Boolean): String {
        val throwUnexpected = fun(): Nothing {
            throw RuntimeException("Unexpected error: The validate function should have checked that already.")
        }

        // GROUP BY (the validate function already ensured that we have exactly "GROUP BY mutation")
        //   -> Aggregate / count mutations
        val siloAction = if (isNucleotide) {
            SiloAction.mutations(0.0)
        } else {
            SiloAction.aminoAcidMutations(0.0)
        }
        val siloQuery = SiloQuery(siloAction, query.whereQueryExpr ?: True)
        var result = this.siloClient.sendQuery(siloQuery)

        // HAVING -> another round of filtering
        val having = query.havingExpression as ComparisonOperator?
        if (having != null) {
            var leftName = having.leftExpression.toString()
            val right = having.rightExpression
            if (query.aliasToExpression.containsKey(leftName)) {
                leftName = query.aliasToExpression[leftName]!!
            }

            val getValueFunc: (MutationData) -> Double = when (leftName) {
                "count(*)" -> {
                    { it.count.toDouble() }
                }

                "proportion()" -> {
                    { it.proportion }
                }

                else -> throwUnexpected()
            }
            val value = when (right) {
                is LongValue -> right.value.toDouble()
                is DoubleValue -> right.value
                else -> throwUnexpected()
            }

            // Now, actually start filtering
            result = result.filter {
                when (having) {
                    is EqualsTo -> getValueFunc(it) == value
                    is NotEqualsTo -> getValueFunc(it) != value
                    is GreaterThan -> getValueFunc(it) > value
                    is GreaterThanEquals -> getValueFunc(it) >= value
                    is MinorThan -> getValueFunc(it) < value
                    is MinorThanEquals -> getValueFunc(it) <= value
                    else -> throwUnexpected()
                }
            }
        }

        // ORDER BY
        val orderBy = query.orderByExpression
        if (orderBy != null) {
            var comparator: Comparator<MutationData> = when (orderBy) {
                "count(*)" -> Comparator.comparingInt { it.count }
                "proportion()" -> Comparator.comparingDouble { it.proportion }
                else -> throwUnexpected()
            }
            if (!query.orderByAsc) {
                comparator = comparator.reversed()
            }
            result = result.sortedWith(comparator)
        }

        // OFFSET and LIMIT
        result = limitAndOffset(result, query)

        return objectMapper.writeValueAsString(result)
    }

    private fun <T> limitAndOffset(data: List<T>, query: Query): List<T> {
        // OFFSET
        var d = data
        val offset = query.offset
        if (offset != null) {
            d = d.subList(Math.min(offset, d.size), d.size)
        }

        // LIMIT
        val limit = query.limit
        if (limit != null) {
            d = d.subList(0, Math.min(limit, d.size))
        }

        return d
    }

}
