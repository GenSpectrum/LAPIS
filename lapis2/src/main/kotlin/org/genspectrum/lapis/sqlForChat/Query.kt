package org.genspectrum.lapis.sqlForChat

import net.sf.jsqlparser.expression.Expression
import org.genspectrum.lapis.silo.SiloFilterExpression

data class Query (
    var table: String? = null,
    var aliasToExpression: Map<String, String> = mutableMapOf(),
    var selectExpressions: MutableList<String> = mutableListOf(),
    var whereExpression: Expression? = null, // The jsqlparser version of the expression
    var whereQueryExpr: SiloFilterExpression? = null, // Our version of the expression
    var groupByColumns: MutableList<String> = mutableListOf(),
    var havingExpression: Expression? = null,
    var orderByExpression: String? = null,
    var orderByAsc: Boolean = true,
    var offset: Int? = null,
    var limit: Int? = null,
)
