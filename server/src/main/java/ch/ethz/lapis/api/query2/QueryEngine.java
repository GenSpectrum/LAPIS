package ch.ethz.lapis.api.query2;

import ch.ethz.lapis.api.VariantQueryListener;
import ch.ethz.lapis.api.entity.AggregationField;
import ch.ethz.lapis.api.entity.req.SampleAggregatedRequest;
import ch.ethz.lapis.api.entity.res.SampleAggregated;
import ch.ethz.lapis.api.parser.VariantQueryLexer;
import ch.ethz.lapis.api.parser.VariantQueryParser;
import ch.ethz.lapis.api.query.*;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static ch.ethz.lapis.api.query2.Database.Columns.*;

public class QueryEngine {

    public List<SampleAggregated> aggregate(Database database, SampleAggregatedRequest request) {
        // Filter variant
        var nucMutations = request.getNucMutations();
        var useNucMutations = nucMutations != null && !nucMutations.isEmpty();
        var aaMutations = request.getAaMutations();
        var useAaMutations = aaMutations != null && !aaMutations.isEmpty();
        var pangoLineage = request.getPangoLineage();
        var usePangoLineage = pangoLineage != null;
        var gisaidClade = request.getGisaidClade();
        var useGisaidClade = gisaidClade != null;
        var nextstrainClade = request.getNextstrainClade();
        var useNextstrainClade = nextstrainClade != null;
        var variantQuery = request.getVariantQuery();
        var useVariantQuery = variantQuery != null;

        boolean useOtherVariantSpecifying = useNucMutations || useAaMutations || usePangoLineage || useGisaidClade
            || useNextstrainClade;
        if (useVariantQuery && useOtherVariantSpecifying) {
            throw new RuntimeException("It is not allowed to use variantQuery and another variant-specifying " +
                "field at the same time.");
        }

        VariantQueryExpr variantQueryExpr = null;
        if (useVariantQuery) {
            variantQueryExpr = parseVariantQueryExpr(variantQuery);
        } else if (useOtherVariantSpecifying) {
            List<VariantQueryExpr> components = new ArrayList<>();
            if (usePangoLineage) {
                PangoQuery pq;
                if (pangoLineage.endsWith("*")) {
                    pq = new PangoQuery(pangoLineage.substring(0, pangoLineage.length() - 1), true);
                } else {
                    pq = new PangoQuery(pangoLineage, false);
                }
                components.add(pq);
            }
            if (useNextstrainClade) {
                components.add(new NextstrainClade(nextstrainClade));
            }
            if (useGisaidClade) {
                components.add(new GisaidClade(gisaidClade));
            }
            if (useAaMutations) {
                components.addAll(aaMutations);
            }
            if (useNucMutations) {
                components.addAll(nucMutations);
            }
            variantQueryExpr = components.get(0);
            for (int i = 1; i < components.size(); i++) {
                BiOp conjunction = new BiOp(BiOp.OpType.AND);
                conjunction.putValue(variantQueryExpr);
                conjunction.putValue(components.get(i));
                variantQueryExpr = conjunction;
            }
        }

        int numberRows = database.size();
        boolean[] matched;
        if (variantQueryExpr != null) {
            matched = variantQueryExpr.evaluate2(database);
        } else {
            matched = new boolean[numberRows];
            Arrays.fill(matched, true);
        }

        // Filter metadata
        between(matched, database.getIntColumn(DATE), request.getDateFrom(), request.getDateTo());
        between(matched, database.getIntColumn(DATE_SUBMITTED),
            request.getDateSubmittedFrom(), request.getDateSubmittedTo());
        eq(matched, database.getStringColumn(REGION), request.getRegion(), true);
        eq(matched, database.getStringColumn(COUNTRY), request.getCountry(), true);
        eq(matched, database.getStringColumn(DIVISION), request.getDivision(), true);
        eq(matched, database.getStringColumn(LOCATION), request.getLocation(), true);
        eq(matched, database.getStringColumn(REGION_EXPOSURE), request.getRegionExposure(), true);
        eq(matched, database.getStringColumn(COUNTRY_EXPOSURE), request.getCountryExposure(), true);
        eq(matched, database.getStringColumn(DIVISION_EXPOSURE), request.getDivisionExposure(), true);
        between(matched, database.getIntColumn(AGE), request.getAgeFrom(), request.getAgeTo());
        eq(matched, database.getStringColumn(SEX), request.getSex(), true);
        eq(matched, database.getBoolColumn(HOSPITALIZED), request.getHospitalized());
        eq(matched, database.getBoolColumn(DIED), request.getDied());
        eq(matched, database.getBoolColumn(FULLY_VACCINATED), request.getFullyVaccinated());
        eq(matched, database.getStringColumn(HOST), request.getHost(), true);
        eq(matched, database.getStringColumn(SAMPLING_STRATEGY), request.getSamplingStrategy(), true);
        eq(matched, database.getStringColumn(SUBMITTING_LAB), request.getSubmittingLab(), true);
        eq(matched, database.getStringColumn(ORIGINATING_LAB), request.getOriginatingLab(), true);

        // Group by
        List<SampleAggregated> result = new ArrayList<>();
        List<AggregationField> fields = request.getFields();
        if (fields.isEmpty()) {
            int count = 0;
            for (int i = 0; i < numberRows; i++) {
                if (matched[i]) {
                    ++count;
                }
            }
            result.add(new SampleAggregated().setCount(count));
        } else {
            Map<List<Object>, int[]> counts = new HashMap<>();
            for (int i = 0; i < numberRows; i++) {
                if (matched[i]) {
                    int finalI = i;
                    List<Object> key = fields.stream()
                        .map(f -> database.getColumn(aggregationFieldToColumnName(f))[finalI])
                        .collect(Collectors.toList());
                    counts.compute(key, (k, v) -> v == null ?
                        new int[] { 0 } : v)[0]++;
                }
            }
            for (Map.Entry<List<Object>, int[]> entry : counts.entrySet()) {
                SampleAggregated sampleAggregated = new SampleAggregated().setCount(entry.getValue()[0]);
                List<Object> key = entry.getKey();
                for (int i = 0; i < fields.size(); i++) {
                    switch (fields.get(i)) {
                        case DATE -> sampleAggregated.setDate(Database.intToDate((Integer) key.get(i)));
                        case DATESUBMITTED -> sampleAggregated
                            .setDateSubmitted(Database.intToDate((Integer) key.get(i)));
                        case REGION -> sampleAggregated.setRegion((String) key.get(i));
                        case COUNTRY -> sampleAggregated.setCountry((String) key.get(i));
                        case DIVISION -> sampleAggregated.setDivision((String) key.get(i));
                        case LOCATION -> sampleAggregated.setLocation((String) key.get(i));
                        case REGIONEXPOSURE -> sampleAggregated.setRegionExposure((String) key.get(i));
                        case COUNTRYEXPOSURE -> sampleAggregated.setCountryExposure((String) key.get(i));
                        case DIVISIONEXPOSURE -> sampleAggregated.setDivisionExposure((String) key.get(i));
                        case AGE -> sampleAggregated.setAge((Integer) key.get(i));
                        case SEX -> sampleAggregated.setSex((String) key.get(i));
                        case HOSPITALIZED -> sampleAggregated.setHospitalized((Boolean) key.get(i));
                        case DIED -> sampleAggregated.setDied((Boolean) key.get(i));
                        case FULLYVACCINATED -> sampleAggregated.setFullyVaccinated((Boolean) key.get(i));
                        case HOST -> sampleAggregated.setHost((String) key.get(i));
                        case SAMPLINGSTRATEGY -> sampleAggregated.setSamplingStrategy((String) key.get(i));
                        case PANGOLINEAGE -> sampleAggregated.setPangoLineage((String) key.get(i));
                        case NEXTSTRAINCLADE -> sampleAggregated.setNextstrainClade((String) key.get(i));
                        case GISAIDCLADE -> sampleAggregated.setGisaidCloade((String) key.get(i));
                        case SUBMITTINGLAB -> sampleAggregated.setSubmittingLab((String) key.get(i));
                        case ORIGINATINGLAB -> sampleAggregated.setOriginatingLab((String) key.get(i));
                    }
                }
                result.add(sampleAggregated);
            }
        }

        return result;
    }

    private void eq(boolean[] matched, String[] data, String value, boolean caseSensitive) {
        if (value == null) {
            return;
        }
        if (caseSensitive) {
            for (int i = 0; i < matched.length; i++) {
                matched[i] = matched[i] && value.equals(data[i]);
            }
        } else {
            for (int i = 0; i < matched.length; i++) {
                matched[i] = matched[i] && value.equalsIgnoreCase(data[i]);
            }
        }
    }

    private void eq(boolean[] matched, Boolean[] data, Boolean value) {
        if (value == null) {
            return;
        }
        for (int i = 0; i < matched.length; i++) {
            matched[i] = matched[i] && data[i] != null && value == data[i];
        }
    }

    private void between(boolean[] matched, Integer[] data, Integer from, Integer to) {
        if (from == null && to == null) {
            return;
        }
        if (from != null && to != null) {
            for (int i = 0; i < matched.length; i++) {
                matched[i] = matched[i] && data[i] != null && data[i] >= from && data[i] <= to;
            }
        } else if (from != null) {
            for (int i = 0; i < matched.length; i++) {
                matched[i] = matched[i] && data[i] != null && data[i] >= from;
            }
        } else {
            for (int i = 0; i < matched.length; i++) {
                matched[i] = matched[i] && data[i] != null && data[i] <= to;
            }
        }
    }

    private void between(boolean[] matched, Integer[] data, LocalDate dateFrom, LocalDate dateTo) {
        Integer dateFromInt = Database.dateToInt(dateFrom);
        Integer dateToInt = Database.dateToInt(dateTo);
        between(matched, data, dateFromInt, dateToInt);
    }

    private VariantQueryExpr parseVariantQueryExpr(String variantQuery) {
        VariantQueryLexer lexer = new VariantQueryLexer(CharStreams.fromString(variantQuery.toUpperCase()));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        VariantQueryParser parser = new VariantQueryParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(ThrowingErrorListener.INSTANCE);
        ParseTree tree = parser.start();
        ParseTreeWalker walker = new ParseTreeWalker();
        VariantQueryListener listener = new VariantQueryListener();
        walker.walk(listener, tree);
        return listener.getExpr();
    }

    private String aggregationFieldToColumnName(AggregationField field) {
        return switch (field) {
            case DATE -> DATE;
            case DATESUBMITTED -> DATE_SUBMITTED;
            case REGION -> REGION;
            case COUNTRY -> COUNTRY;
            case DIVISION -> DIVISION;
            case LOCATION -> LOCATION;
            case REGIONEXPOSURE -> REGION_EXPOSURE;
            case COUNTRYEXPOSURE -> COUNTRY_EXPOSURE;
            case DIVISIONEXPOSURE -> DIVISION_EXPOSURE;
            case AGE -> AGE;
            case SEX -> SEX;
            case HOSPITALIZED -> HOSPITALIZED;
            case DIED -> DIED;
            case FULLYVACCINATED -> FULLY_VACCINATED;
            case HOST -> HOST;
            case SAMPLINGSTRATEGY -> SAMPLING_STRATEGY;
            case PANGOLINEAGE -> PANGO_LINEAGE;
            case NEXTSTRAINCLADE -> NEXTSTRAIN_CLADE;
            case GISAIDCLADE -> GISAID_CLADE;
            case SUBMITTINGLAB -> SUBMITTING_LAB;
            case ORIGINATINGLAB -> ORIGINATING_LAB;
            default -> throw new IllegalStateException("Unexpected value: " + field);
        };
    }

}
