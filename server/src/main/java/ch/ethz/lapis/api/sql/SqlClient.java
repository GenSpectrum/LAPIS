package ch.ethz.lapis.api.sql;

import ch.ethz.lapis.api.query.Database;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import org.javatuples.Pair;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SqlClient {

    /**
     * Parses a SQL and checks that only supported SQL constructs are used. There is one limitation: it does not check
     * the content of the WHERE and HAVING clauses. This shall be done by the validate() function.
     */
    public Query parse(String sql) {
        try {
            Query query = new Query();

            // Check that it is a plain SELECT statement with not more than following top-level keywords:
            // select, from, where, group by, having, order by, limit, offset
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (!(statement instanceof Select selectStatement)) throw new UnsupportedSqlException();
            if (selectStatement.getWithItemsList() != null || selectStatement.isUsingWithBrackets()) {
                throw new UnsupportedSqlException();
            }
            var selectBody = selectStatement.getSelectBody();
            if (!(selectBody instanceof PlainSelect plainSelect)) throw new UnsupportedSqlException();
            if (plainSelect.getDistinct() != null ||
                plainSelect.getIntoTables() != null ||
                plainSelect.getJoins() != null ||
                plainSelect.getFetch() != null ||
                plainSelect.getOptimizeFor() != null ||
                plainSelect.getSkip() != null ||
                plainSelect.getMySqlHintStraightJoin() ||
                plainSelect.getTop() != null ||
                plainSelect.getOracleHierarchical() != null ||
                plainSelect.getOracleHint() != null ||
                plainSelect.isOracleSiblings() ||
                plainSelect.isForUpdate() ||
                plainSelect.getForUpdateTable() != null ||
                plainSelect.isSkipLocked() ||
                plainSelect.isUseBrackets() ||
                plainSelect.getWait() != null ||
                plainSelect.getMySqlSqlCalcFoundRows() ||
                plainSelect.getMySqlSqlCacheFlag() != null ||
                plainSelect.getForXmlPath() != null ||
                plainSelect.getKsqlWindow() != null ||
                plainSelect.isNoWait() ||
                plainSelect.isEmitChanges() ||
                plainSelect.getWithIsolation() != null ||
                plainSelect.getWindowDefinitions() != null
            ) {
                throw new UnsupportedSqlException();
            }

            // SELECT
            var selectItems = plainSelect.getSelectItems();
            var selectExpressionsAndAliases = extractSelectExpressionsAndAliases(selectItems);
            query.setAliasToExpression(selectExpressionsAndAliases.getValue0());
            query.setSelectExpressions(selectExpressionsAndAliases.getValue1());

            // FROM
            // Only accept a single table
            var fromItem = plainSelect.getFromItem();
            if (!(fromItem instanceof Table)) throw new UnsupportedSqlException();
            query.setTable(fromItem.toString());

            // WHERE
            // No further validation and processing at the moment
            query.setWhereExpression(plainSelect.getWhere());

            // GROUP BY
            var groupBy = plainSelect.getGroupBy();
            if (groupBy != null) {
                var expressionList = groupBy.getGroupByExpressionList();
                var expressions = expressionList.getExpressions();
                for (Expression expression : expressions) {
                    if (!(expression instanceof Column column)) throw new UnsupportedSqlException();
                    query.getGroupByColumns().add(column.getColumnName());
                }
            }

            // HAVING
            // No further validation and processing at the moment
            query.setHavingExpression(plainSelect.getHaving());

            // ORDER BY
            var orderByElements = plainSelect.getOrderByElements();
            if (orderByElements != null && orderByElements.size() > 0) {
                // Don't want more than 1 expression at the moment because it gets to complicated...
                if (orderByElements.size() > 1) throw new UnsupportedSqlException();
                var orderByElement = orderByElements.get(0);
                var expression = orderByElement.getExpression();
                if (expression instanceof Column column && column.getTable() == null) {
                    query.setOrderByExpression(column.getColumnName());
                } else if (expression instanceof Function function) {
                    query.setOrderByExpression(function.toString());
                } else {
                    throw new UnsupportedSqlException();
                }
                query.setOrderByAsc(orderByElement.isAsc());
            }

            // OFFSET
            var offset = plainSelect.getOffset();
            if (offset != null) {
                if (!(offset.getOffset() instanceof LongValue offsetValue) || offset.getOffsetParam() != null) {
                    throw new UnsupportedSqlException();
                }
                query.setOffset((int) offsetValue.getValue());
            }

            // LIMIT
            var limit = plainSelect.getLimit();
            if (limit != null) {
                var rowCount = limit.getRowCount();
                if (rowCount != null) {
                    if (!(rowCount instanceof LongValue rowCountValue)) throw new UnsupportedSqlException();
                    query.setLimit((int) rowCountValue.getValue());
                }
                // LIMIT with offset
                var limitOffsetExpression = limit.getOffset();
                if (limit.getOffset() != null) {
                    if (!(limitOffsetExpression instanceof LongValue offsetValue)) throw new UnsupportedSqlException();
                    int offsetInteger = (int) offsetValue.getValue();
                    // TODO Why should someone provide offset twice and with different values? It's probably just wrong.
                    //    But maybe it has a semantic that I am not aware of?
                    if (query.getOffset() != null && query.getOffset() != offsetInteger) {
                        throw new UnsupportedSqlException();
                    }
                    query.setOffset(offsetInteger);
                }
            }

            return query;
        } catch (JSQLParserException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * <ol>
     *     <li>Check the fields (existence and compatibility)</li>
     *     <li>Rewrite: Use "nextcladePangoLineage"</li>
     * </ol>
     */
    public void validateAndRewrite(Query query) {
        // Table
        String table = query.getTable();
        if (!table.equals("metadata") && !table.equals("aa_mutations") && !table.equals("nuc_mutations")) {
            throw new UnsupportedSqlException();
        }

        // SELECT, GROUP BY, and ORDER BY
        if (table.equals("metadata")) {
            validateAndRewriteMetadataQuery(query);
        } else {
            validateAndRewriteMutationQuery(query);
        }

        // WHERE
        var where = query.getWhereExpression();
        if (where != null) {
            validateAndRewriteWhereExpression(where);
        }
    }

    private Pair<Map<String, String>, List<String>> extractSelectExpressionsAndAliases(List<SelectItem> selectItems) {
        Map<String, String> aliasToExpression = new HashMap<>();
        List<String> selectExpressions = new ArrayList<>();
        for (SelectItem selectItem : selectItems) {
            if (!(selectItem instanceof SelectExpressionItem selectExprItem)) throw new UnsupportedSqlException();
            var expression = selectExprItem.getExpression();
            String selectExpression;
            if (expression instanceof Column column && column.getTable() == null) {
                selectExpression = column.getColumnName();
            } else if (expression instanceof Function function) {
                selectExpression = function.toString();
            } else {
                throw new UnsupportedSqlException();
            }
            if (selectExprItem.getAlias() != null && selectExprItem.getAlias().getName() != null) {
                aliasToExpression.put(selectExprItem.getAlias().getName(), selectExpression);
            }
            selectExpressions.add(selectExpression);
        }
        return new Pair<>(aliasToExpression, selectExpressions);
    }

    private void validateAndRewriteMetadataQuery(Query query) {
        // SELECT: only allow metadata fields and count(*)
        List<String> newSelectExpressions = new ArrayList<>();
        for (String selectExpression : query.getSelectExpressions()) {
            String metadataField = validateAndRewriteMetadataField(selectExpression);
            if (metadataField != null) {
                newSelectExpressions.add(metadataField);
            } else if (selectExpression.equals("count(*)")) {
                newSelectExpressions.add(selectExpression);
            } else {
                throw new UnsupportedSqlException();
            }
        }
        query.setSelectExpressions(newSelectExpressions);

        // GROUP BY: only allow metadata fields
        List<String> newGroupByColumns = new ArrayList<>();
        for (String groupByColumn : query.getGroupByColumns()) {
            String metadataField = validateAndRewriteMetadataField(groupByColumn);
            if (metadataField != null) {
                newGroupByColumns.add(metadataField);
            } else {
                throw new UnsupportedSqlException();
            }
        }
        query.setGroupByColumns(newGroupByColumns);

        // HAVING
        // Only a simple comparison is supported with count(*) on the left and constant on the right
        var having = query.getHavingExpression();
        if (having != null) {
            if (!(having instanceof ComparisonOperator comparison)) throw new UnsupportedSqlException();
            var leftName = comparison.getLeftExpression().toString();
            var right = comparison.getRightExpression();
            if (query.getAliasToExpression().containsKey(leftName)) {
                leftName = query.getAliasToExpression().get(leftName);
            }
            if (!leftName.equals("count(*)")) throw new UnsupportedSqlException();
            if (!(right instanceof LongValue || right instanceof DoubleValue)) throw new UnsupportedSqlException();
            if (comparison instanceof GeometryDistance) throw new UnsupportedSqlException(); // Other comparisons are OK
        }

        // ORDER BY: it must be a metadata field mentioned in GROUP BY or count(*)
        String orderBy = query.getOrderByExpression();
        if (orderBy != null) {
            // If an alias is used, it will be resolved and replaced.
            if (query.getAliasToExpression().containsKey(orderBy)) {
                orderBy = query.getAliasToExpression().get(orderBy);
                query.setOrderByExpression(orderBy);
            }
            String metadataField = validateAndRewriteMetadataField(orderBy);
            if (metadataField != null) {
                if (!query.getGroupByColumns().contains(metadataField)) throw new UnsupportedSqlException();
            } else if (!orderBy.equals("count(*)")) {
                throw new UnsupportedSqlException();
            }
        }
    }

    private void validateAndRewriteMutationQuery(Query query) {
        // SELECT: only allow "mutation", count(*), and proportion()
        for (String selectExpression : query.getSelectExpressions()) {
            if (!selectExpression.equals("mutation") && !selectExpression.equals("count(*)") &&
                !selectExpression.equals("proportion()")) {
                throw new UnsupportedSqlException();
            }
        }

        // GROUP BY: only allow "mutation"
        for (String groupByColumn : query.getGroupByColumns()) {
            if (!groupByColumn.equals("mutation")) {
                throw new UnsupportedSqlException();
            }
        }

        // HAVING
        // Only a simple comparison is supported with count(*) or proportion() on the left and constant on the right
        var having = query.getHavingExpression();
        if (having != null) {
            if (!(having instanceof ComparisonOperator comparison)) throw new UnsupportedSqlException();
            var leftName = comparison.getLeftExpression().toString();
            var right = comparison.getRightExpression();
            if (query.getAliasToExpression().containsKey(leftName)) {
                leftName = query.getAliasToExpression().get(leftName);
            }
            if (!leftName.equals("count(*)") && !leftName.equals("proportion()")) throw new UnsupportedSqlException();
            if (!(right instanceof LongValue || right instanceof DoubleValue)) throw new UnsupportedSqlException();
            if (comparison instanceof GeometryDistance) throw new UnsupportedSqlException(); // Other comparisons are OK
        }

        // ORDER BY: it must be "mutation", count(*), or proportion()
        String orderBy = query.getOrderByExpression();
        if (orderBy != null) {
            // If an alias is used, it will be resolved and replaced.
            if (query.getAliasToExpression().containsKey(orderBy)) {
                orderBy = query.getAliasToExpression().get(orderBy);
                query.setOrderByExpression(orderBy);
            }
            if (!(orderBy.equals("mutation") || orderBy.equals("count(*)") || orderBy.equals("proportion()"))) {
                throw new UnsupportedSqlException();
            }
        }
    }

    private void validateAndRewriteWhereExpression(Expression expression) {
        if (expression instanceof AndExpression and) {
            validateAndRewriteWhereExpression(and.getLeftExpression());
            validateAndRewriteWhereExpression(and.getRightExpression());
        } else if (expression instanceof OrExpression or) {
            validateAndRewriteWhereExpression(or.getLeftExpression());
            validateAndRewriteWhereExpression(or.getRightExpression());
        } else if (expression instanceof NotExpression not) {
            validateAndRewriteWhereExpression(not.getExpression());
        } else if (expression instanceof ComparisonOperator comparison) {
            var left = comparison.getLeftExpression();
            var right = comparison.getRightExpression();

            // The left side must be a column
            if (!(left instanceof Column column)) throw new UnsupportedSqlException();
            String columnName = column.getColumnName();

            // It can be a metadata field or a mutation column encoded as nuc_123 or aa_S_234
            String metadataColumn = validateAndRewriteMetadataField(columnName);
            if (metadataColumn != null) {
                column.setColumnName(metadataColumn);
                // The right side must be a constant value. The type of the value must fit the column.
                // The operator must also make sense.
                String columnType = Database.COLUMN_TO_TYPE.get(metadataColumn);
                switch (columnType) {
                    case "string" -> {
                        if (!(right instanceof StringValue)) throw new UnsupportedSqlException();
                        if (!(comparison instanceof EqualsTo || comparison instanceof NotEqualsTo))
                            throw new UnsupportedSqlException();
                    }
                    case "date" -> {
                        if (!(right instanceof StringValue stringValue)) throw new UnsupportedSqlException();
                        try {
                            LocalDate.parse(stringValue.getValue());
                        } catch (DateTimeParseException e) {
                            throw new UnsupportedSqlException();
                        }
                        if (!(comparison instanceof EqualsTo || comparison instanceof NotEqualsTo ||
                            comparison instanceof GreaterThan || comparison instanceof GreaterThanEquals ||
                            comparison instanceof MinorThan || comparison instanceof MinorThanEquals))
                            throw new UnsupportedSqlException();
                    }
                    default -> throw new UnsupportedSqlException(); // Due to laziness
                }
            } else if (columnName.startsWith("nuc_") || columnName.startsWith("aa_")) {
                // The right side must be a constant string value and only (not)equals is supported.
                if (!(right instanceof StringValue)) throw new UnsupportedSqlException();
                if (!(comparison instanceof EqualsTo || comparison instanceof NotEqualsTo))
                    throw new UnsupportedSqlException();
            }
        } else if (expression instanceof Between between) {
            var left = between.getLeftExpression();
            var rightStart = between.getBetweenExpressionStart();
            var rightEnd = between.getBetweenExpressionEnd();

            // The left side must be a column
            if (!(left instanceof Column column)) throw new UnsupportedSqlException();
            String columnName = column.getColumnName();

            // We only support dates (integer and float are not supported due to laziness)
            String columnType = Database.COLUMN_TO_TYPE.get(columnName);
            if (!"date".equals(columnType)) throw new UnsupportedSqlException();

            // The values on the right side must be constants and valid dates.
            if (!(rightStart instanceof StringValue startString) || !(rightEnd instanceof StringValue endString))
                throw new UnsupportedSqlException();
            try {
                LocalDate.parse(startString.getValue());
                LocalDate.parse(endString.getValue());
            } catch (DateTimeParseException e) {
                throw new UnsupportedSqlException();
            }
        } else {
            throw new UnsupportedSqlException();
        }
    }

    /**
     * Returns a valid metadata field name or null
     */
    private String validateAndRewriteMetadataField(String field) {
        if (Database.COLUMN_TO_TYPE.containsKey(field)) {
            return field;
        }
        if (field.toLowerCase().contains("lineage")) {
            return "nextclade_pango_lineage";
        }
        return null;
    }

}
