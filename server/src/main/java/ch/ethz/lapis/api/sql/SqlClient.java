package ch.ethz.lapis.api.sql;

import ch.ethz.lapis.api.entity.AAMutation;
import ch.ethz.lapis.api.entity.AggregationField;
import ch.ethz.lapis.api.entity.NucMutation;
import ch.ethz.lapis.api.entity.SequenceType;
import ch.ethz.lapis.api.entity.res.SampleAggregated;
import ch.ethz.lapis.api.entity.res.SampleAggregatedResponse;
import ch.ethz.lapis.api.entity.res.SampleMutationsResponse;
import ch.ethz.lapis.api.query.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.StringValue;
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
import java.util.*;

import static ch.ethz.lapis.api.query.Database.Columns.*;

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
            if (// The distinct keyword shall be ignored for now. It shouldn't be a problem in most cases as we
                // copy over the values in SELECT into GROUP BY
                //plainSelect.getDistinct() != null ||
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
                    if (query.getOffset() != null && query.getOffset() != offsetInteger) {
                        // TODO Why should someone provide offset twice and with different values? It's probably just
                        //  wrong. But maybe it has a semantic that I am not aware of?
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
     *     <li>Rewrite: resolve aliases in ORDER BY (but not in HAVING)</li>
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
            query.setWhereQueryExpr(validateAndRewriteWhereExpression(where));
        }
    }

    public String executeToJson(Query query, Database database, ObjectMapper objectMapper) {
        // WHERE -> filter the sequences
        var where = query.getWhereQueryExpr();
        boolean[] sequencesMatched;
        if (where != null) {
            sequencesMatched = where.evaluate(database);
        } else {
            sequencesMatched = new boolean[database.size()];
            Arrays.fill(sequencesMatched, true);
        }

        String result = switch (query.getTable()) {
            case "metadata" -> executeMetadataQuery(query, database, objectMapper, sequencesMatched);
            case "nuc_mutations" ->
                executeMutationsQuery(query, database, objectMapper, sequencesMatched, SequenceType.NUCLEOTIDE);
            case "aa_mutations" ->
                executeMutationsQuery(query, database, objectMapper, sequencesMatched, SequenceType.AMINO_ACID);
            default -> throw new RuntimeException("Unexpected error");
        };

        return result;
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
        List<String> oldGroupByColumns = query.getGroupByColumns();
        // This is now a very "opinionated" rewrite: because we do not allow querying of individual samples,
        // we can't evaluate something like "select date from metadata order by date limit 1". As a hack,
        // such a query will be rewritten to "select date from metadata group by date order by date limit 1".
        // More generally, we will add all metadata columns occurring in SELECT to GROUP BY.
        // This might return something different from what the user wants but hopefully, it's in most cases not to far.
        oldGroupByColumns.addAll(newSelectExpressions.stream().filter(e -> !e.equals("count(*)")).toList());
        oldGroupByColumns = new ArrayList<>(new HashSet<>(oldGroupByColumns));
        List<String> newGroupByColumns = new ArrayList<>();
        for (String groupByColumn : oldGroupByColumns) {
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

        // GROUP BY: it must be grouped by "mutation" and only by "mutation.
        var groupByColumns = query.getGroupByColumns();
        if (groupByColumns.size() != 1 || !groupByColumns.get(0).equals("mutation")) {
            throw new UnsupportedSqlException();
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

    private QueryExpr validateAndRewriteWhereExpression(Expression expression) {
        if (expression instanceof AndExpression and) {
            var leftExpr = validateAndRewriteWhereExpression(and.getLeftExpression());
            var rightExpr = validateAndRewriteWhereExpression(and.getRightExpression());
            var result = new BiOp(BiOp.OpType.AND);
            result.putValue(leftExpr);
            result.putValue(rightExpr);
            return result;
        } else if (expression instanceof OrExpression or) {
            var leftExpr = validateAndRewriteWhereExpression(or.getLeftExpression());
            var rightExpr = validateAndRewriteWhereExpression(or.getRightExpression());
            var result = new BiOp(BiOp.OpType.OR);
            result.putValue(leftExpr);
            result.putValue(rightExpr);
            return result;
        } else if (expression instanceof NotExpression not) {
            var expr = validateAndRewriteWhereExpression(not.getExpression());
            var result = new Negation();
            result.putValue(expr);
            return result;
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
                        if (!(right instanceof StringValue value)) throw new UnsupportedSqlException();
                        QueryExpr expr;
                        if (metadataColumn.equals(NEXTCLADE_PANGO_LINEAGE)) {
                            expr = new PangoQuery(value.getValue(), true, NEXTCLADE_PANGO_LINEAGE);
                        } else {
                            expr = new ch.ethz.lapis.api.query.StringValue(value.getValue(), metadataColumn, false);
                        }
                        if (comparison instanceof EqualsTo) {
                            return expr;
                        } else if (comparison instanceof NotEqualsTo) {
                            var negation = new Negation();
                            negation.putValue(expr);
                            return negation;
                        } else {
                            throw new UnsupportedSqlException();
                        }
                    }
                    case "date" -> {
                        if (!(right instanceof StringValue stringValue)) throw new UnsupportedSqlException();
                        LocalDate date;
                        try {
                            date = LocalDate.parse(stringValue.getValue());
                        } catch (DateTimeParseException e) {
                            throw new UnsupportedSqlException();
                        }
                        if (comparison instanceof EqualsTo) {
                            return new DateCompare(DateCompare.OpType.EQUAL, metadataColumn, date);
                        } else if (comparison instanceof NotEqualsTo) {
                            var negation = new Negation();
                            negation.putValue(new DateCompare(DateCompare.OpType.EQUAL, metadataColumn, date));
                            return negation;
                        } else if (comparison instanceof GreaterThan) {
                            return new DateCompare(DateCompare.OpType.GREATER_THAN, metadataColumn, date);
                        } else if (comparison instanceof GreaterThanEquals) {
                            return new DateCompare(DateCompare.OpType.GREATER_THAN_EQUAL, metadataColumn, date);
                        } else if (comparison instanceof MinorThan) {
                            return new DateCompare(DateCompare.OpType.LESS_THAN, metadataColumn, date);
                        } else if (comparison instanceof MinorThanEquals) {
                            return new DateCompare(DateCompare.OpType.LESS_THAN_EQUAL, metadataColumn, date);
                        } else {
                            throw new UnsupportedSqlException();
                        }
                    }
                    default -> throw new UnsupportedSqlException(); // Due to laziness
                }
            } else if (columnName.startsWith("nuc_")) {
                var parts = columnName.split("_");
                if (parts.length != 2) throw new UnsupportedSqlException();
                var positionStr = parts[1];
                int position;
                try {
                    position = Integer.parseUnsignedInt(positionStr);
                } catch (NumberFormatException e) {
                    throw new UnsupportedSqlException();
                }

                // The right side must be a constant string value
                if (!(right instanceof StringValue value)) throw new UnsupportedSqlException();
                String value2 = value.getValue();
                if (value2.length() != 1) throw new UnsupportedSqlException();
                char value3 = value2.charAt(0);

                NucMutation nucMutation = new NucMutation(position, value3);
                if (comparison instanceof EqualsTo) {
                    return nucMutation;
                } else if (comparison instanceof NotEqualsTo) {
                    var negation = new Negation();
                    negation.putValue(nucMutation);
                    return negation;
                } else {
                    throw new UnsupportedSqlException();
                }
            } else if (columnName.startsWith("aa_")) {
                var parts = columnName.split("_");
                if (parts.length != 3) throw new UnsupportedSqlException();
                String gene = parts[1];
                String positionStr = parts[2];
                int position;
                try {
                    position = Integer.parseUnsignedInt(positionStr);
                } catch (NumberFormatException e) {
                    throw new UnsupportedSqlException();
                }

                // The right side must be a constant string value
                if (!(right instanceof StringValue value)) throw new UnsupportedSqlException();
                String value2 = value.getValue();
                if (value2.length() != 1) throw new UnsupportedSqlException();
                char value3 = value2.charAt(0);

                AAMutation aaMutation = new AAMutation(gene, position, value3);
                if (comparison instanceof EqualsTo) {
                    return aaMutation;
                } else if (comparison instanceof NotEqualsTo) {
                    var negation = new Negation();
                    negation.putValue(aaMutation);
                    return negation;
                } else {
                    throw new UnsupportedSqlException();
                }
            } else {
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
                var startDate = LocalDate.parse(startString.getValue());
                var endDate = LocalDate.parse(endString.getValue());
                return new DateBetween(columnName, startDate, endDate);
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
            return NEXTCLADE_PANGO_LINEAGE;
        }
        return null;
    }

    private String executeMetadataQuery(Query query, Database database, ObjectMapper objectMapper, boolean[] sequencesMatched) {
        // GROUP BY -> aggregate
        QueryEngine queryEngine = new QueryEngine();
        List<AggregationField> aggregationFields = query.getGroupByColumns().stream()
            .map(QueryEngine::columnNameToaggregationField)
            .toList();
        List<SampleAggregated> result = queryEngine.aggregate(database, aggregationFields, sequencesMatched);

        // HAVING -> another round of filtering
        var having = (ComparisonOperator) query.getHavingExpression();
        if (having != null) {
            var leftName = having.getLeftExpression().toString();
            var right = having.getRightExpression();
            if (query.getAliasToExpression().containsKey(leftName)) {
                leftName = query.getAliasToExpression().get(leftName);
            }
            if (!leftName.equals("count(*)")) {
                throw new RuntimeException("Unexpected error: The validate function should have checked that already.");
            }
            double value;
            if (right instanceof LongValue l) {
                value = l.getValue();
            } else if (right instanceof DoubleValue d) {
                value = d.getValue();
            } else {
                throw new RuntimeException("Unexpected error: The validate function should have checked that already.");
            }

            // Now, actually start filtering
            result = result.stream()
                .filter(sampleAggregated -> {
                    if (having instanceof EqualsTo) {
                        return sampleAggregated.getCount() == value;
                    } else if (having instanceof NotEqualsTo) {
                        return sampleAggregated.getCount() != value;
                    } else if (having instanceof GreaterThan) {
                        return sampleAggregated.getCount() > value;
                    } else if (having instanceof GreaterThanEquals) {
                        return sampleAggregated.getCount() >= value;
                    } else if (having instanceof MinorThan) {
                        return sampleAggregated.getCount() < value;
                    } else if (having instanceof MinorThanEquals) {
                        return sampleAggregated.getCount() <= value;
                    } else {
                        throw new RuntimeException("Unexpected error: The validate function should have checked that already.");
                    }
                })
                .toList();
        }

        // ORDER BY
        var orderBy = query.getOrderByExpression();
        if (orderBy != null) {
            Comparator<SampleAggregated> comparator;
            if (orderBy.equals("count(*)")) {
                comparator = Comparator.comparing(SampleAggregated::getCount);
            } else {
                comparator = switch (orderBy) {
                    case DATE ->
                        Comparator.comparing(SampleAggregated::getDate, Comparator.nullsLast(LocalDate::compareTo));
                    case YEAR ->
                        Comparator.comparing(SampleAggregated::getYear, Comparator.nullsLast(Integer::compareTo));
                    case MONTH ->
                        Comparator.comparing(SampleAggregated::getMonth, Comparator.nullsLast(Integer::compareTo));
                    case DATE_SUBMITTED ->
                        Comparator.comparing(SampleAggregated::getDateSubmitted, Comparator.nullsLast(LocalDate::compareTo));
                    case REGION ->
                        Comparator.comparing(SampleAggregated::getRegion, Comparator.nullsLast(String::compareTo));
                    case COUNTRY ->
                        Comparator.comparing(SampleAggregated::getCountry, Comparator.nullsLast(String::compareTo));
                    case DIVISION ->
                        Comparator.comparing(SampleAggregated::getDivision, Comparator.nullsLast(String::compareTo));
                    case LOCATION ->
                        Comparator.comparing(SampleAggregated::getLocation, Comparator.nullsLast(String::compareTo));
                    case REGION_EXPOSURE ->
                        Comparator.comparing(SampleAggregated::getRegionExposure, Comparator.nullsLast(String::compareTo));
                    case COUNTRY_EXPOSURE ->
                        Comparator.comparing(SampleAggregated::getCountryExposure, Comparator.nullsLast(String::compareTo));
                    case DIVISION_EXPOSURE ->
                        Comparator.comparing(SampleAggregated::getDivisionExposure, Comparator.nullsLast(String::compareTo));
                    case AGE ->
                        Comparator.comparing(SampleAggregated::getAge, Comparator.nullsLast(Integer::compareTo));
                    case SEX -> Comparator.comparing(SampleAggregated::getSex, Comparator.nullsLast(String::compareTo));
                    case HOSPITALIZED ->
                        Comparator.comparing(SampleAggregated::getHospitalized, Comparator.nullsLast(Boolean::compareTo));
                    case DIED ->
                        Comparator.comparing(SampleAggregated::getDied, Comparator.nullsLast(Boolean::compareTo));
                    case FULLY_VACCINATED ->
                        Comparator.comparing(SampleAggregated::getFullyVaccinated, Comparator.nullsLast(Boolean::compareTo));
                    case HOST ->
                        Comparator.comparing(SampleAggregated::getHost, Comparator.nullsLast(String::compareTo));
                    case SAMPLING_STRATEGY ->
                        Comparator.comparing(SampleAggregated::getSamplingStrategy, Comparator.nullsLast(String::compareTo));
                    case PANGO_LINEAGE ->
                        Comparator.comparing(SampleAggregated::getPangoLineage, Comparator.nullsLast(String::compareTo));
                    case NEXTCLADE_PANGO_LINEAGE ->
                        Comparator.comparing(SampleAggregated::getNextcladePangoLineage, Comparator.nullsLast(String::compareTo));
                    case NEXTSTRAIN_CLADE ->
                        Comparator.comparing(SampleAggregated::getNextstrainClade, Comparator.nullsLast(String::compareTo));
                    case GISAID_CLADE ->
                        Comparator.comparing(SampleAggregated::getGisaidCloade, Comparator.nullsLast(String::compareTo));
                    case SUBMITTING_LAB ->
                        Comparator.comparing(SampleAggregated::getSubmittingLab, Comparator.nullsLast(String::compareTo));
                    case ORIGINATING_LAB ->
                        Comparator.comparing(SampleAggregated::getOriginatingLab, Comparator.nullsLast(String::compareTo));
                    case DATABASE ->
                        Comparator.comparing(SampleAggregated::getDatabase, Comparator.nullsLast(String::compareTo));
                    default -> throw new IllegalStateException("Unexpected order by value: " + orderBy);
                };
            }
            if (!query.isOrderByAsc()) {
                comparator = comparator.reversed();
            }
            result = result.stream().sorted(comparator).toList();
        }

        // OFFSET and LIMIT
        result = limitAndOffset(result, query);

        SampleAggregatedResponse response = new SampleAggregatedResponse(aggregationFields, result);
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String executeMutationsQuery(
        Query query,
        Database database,
        ObjectMapper objectMapper,
        boolean[] sequencesMatched,
        SequenceType sequenceType
    ) {
        // GROUP BY (the validate function already ensured that we have exactly "GROUP BY mutation")
        //   -> Aggregate / count mutations
        QueryEngine queryEngine = new QueryEngine();
        List<SampleMutationsResponse.MutationEntry> result = queryEngine.getMutations(database, sequencesMatched, sequenceType, 0);

        // HAVING -> another round of filtering
        var having = (ComparisonOperator) query.getHavingExpression();
        if (having != null) {
            var leftName = having.getLeftExpression().toString();
            var right = having.getRightExpression();
            if (query.getAliasToExpression().containsKey(leftName)) {
                leftName = query.getAliasToExpression().get(leftName);
            }

            java.util.function.Function<SampleMutationsResponse.MutationEntry, Number> getValueFunc;
            if (leftName.equals("count(*)")) {
                getValueFunc = SampleMutationsResponse.MutationEntry::count;
            } else if (leftName.equals("proportion()")) {
                getValueFunc = SampleMutationsResponse.MutationEntry::proportion;
            } else {
                throw new RuntimeException("Unexpected error: The validate function should have checked that already.");
            }

            double value;
            if (right instanceof LongValue l) {
                value = l.getValue();
            } else if (right instanceof DoubleValue d) {
                value = d.getValue();
            } else {
                throw new RuntimeException("Unexpected error: The validate function should have checked that already.");
            }

            // Now, actually start filtering
            result = result.stream()
                .filter(mutationEntry -> {
                    if (having instanceof EqualsTo) {
                        return ((double) getValueFunc.apply(mutationEntry)) == value;
                    } else if (having instanceof NotEqualsTo) {
                        return ((double) getValueFunc.apply(mutationEntry)) != value;
                    } else if (having instanceof GreaterThan) {
                        return ((double) getValueFunc.apply(mutationEntry)) > value;
                    } else if (having instanceof GreaterThanEquals) {
                        return ((double) getValueFunc.apply(mutationEntry)) >= value;
                    } else if (having instanceof MinorThan) {
                        return ((double) getValueFunc.apply(mutationEntry)) < value;
                    } else if (having instanceof MinorThanEquals) {
                        return ((double) getValueFunc.apply(mutationEntry)) <= value;
                    } else {
                        throw new RuntimeException("Unexpected error: The validate function should have checked that already.");
                    }
                })
                .toList();
        }

        // ORDER BY
        var orderBy = query.getOrderByExpression();
        if (orderBy != null) {
            Comparator<SampleMutationsResponse.MutationEntry> comparator;
            if (orderBy.equals("count(*)")) {
                comparator = Comparator.comparing(SampleMutationsResponse.MutationEntry::count);
            } else if (orderBy.equals("proportion()")) {
                comparator = Comparator.comparing(SampleMutationsResponse.MutationEntry::proportion);
            } else {
                throw new RuntimeException("Unexpected error: The validate function should have checked that already.");
            }
            if (!query.isOrderByAsc()) {
                comparator = comparator.reversed();
            }
            result = result.stream().sorted(comparator).toList();
        }

        // OFFSET and LIMIT
        result = limitAndOffset(result, query);

        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> List<T> limitAndOffset(List<T> data, Query query) {
        // OFFSET
        var offset = query.getOffset();
        if (offset != null) {
            data = data.subList(Math.min(offset, data.size()), data.size());
        }

        // LIMIT
        var limit = query.getLimit();
        if (limit != null) {
            data = data.subList(0, Math.min(limit, data.size()));
        }

        return data;
    }

}
