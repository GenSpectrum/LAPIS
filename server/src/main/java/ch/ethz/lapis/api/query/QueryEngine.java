package ch.ethz.lapis.api.query;

import ch.ethz.lapis.api.VariantQueryListener;
import ch.ethz.lapis.api.entity.AggregationField;
import ch.ethz.lapis.api.entity.req.SampleAggregatedRequest;
import ch.ethz.lapis.api.entity.req.SampleFilter;
import ch.ethz.lapis.api.entity.res.SampleAggregated;
import ch.ethz.lapis.api.exception.BadRequestException;
import ch.ethz.lapis.api.exception.MalformedVariantQueryException;
import ch.ethz.lapis.api.parser.VariantQueryLexer;
import ch.ethz.lapis.api.parser.VariantQueryParser;
import ch.ethz.lapis.core.Utils;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ch.ethz.lapis.api.query.Database.Columns.*;
@Slf4j
public class QueryEngine {

    public List<SampleAggregated> aggregate(Database database, SampleAggregatedRequest request) {
        boolean[] matched = matchSampleFilter(database, request);
        List<AggregationField> fields = request.getFields();
        return aggregate(database, fields, matched);
    }

    public List<SampleAggregated> aggregate(Database database, List<AggregationField> fields, boolean[] matched) {
        int numberRows = database.size();

        // Group by
        List<SampleAggregated> result = new ArrayList<>();
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
                    counts.compute(key, (k, v) -> v == null ? new int[]{0} : v)[0]++;
                }
            }
            for (Map.Entry<List<Object>, int[]> entry : counts.entrySet()) {
                SampleAggregated sampleAggregated = new SampleAggregated().setCount(entry.getValue()[0]);
                List<Object> key = entry.getKey();
                for (int i = 0; i < fields.size(); i++) {
                    switch (fields.get(i)) {
                        case DATE -> sampleAggregated.setDate(Database.intToDate((Integer) key.get(i)));
                        case YEAR -> sampleAggregated.setYear((Integer) key.get(i));
                        case MONTH -> sampleAggregated.setMonth((Integer) key.get(i));
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
                        case NEXTCLADEPANGOLINEAGE -> sampleAggregated.setNextcladePangoLineage((String) key.get(i));
                        case NEXTSTRAINCLADE -> sampleAggregated.setNextstrainClade((String) key.get(i));
                        case GISAIDCLADE -> sampleAggregated.setGisaidCloade((String) key.get(i));
                        case SUBMITTINGLAB -> sampleAggregated.setSubmittingLab((String) key.get(i));
                        case ORIGINATINGLAB -> sampleAggregated.setOriginatingLab((String) key.get(i));
                        case DATABASE -> sampleAggregated.setDatabase((String) key.get(i));
                    }
                }
                result.add(sampleAggregated);
            }
        }

        return result;
    }

    public List<Integer> filterIds(Database database, SampleFilter sampleFilter) {
        boolean[] matched = matchSampleFilter(database, sampleFilter);
        return IntStream.range(0, matched.length)
            .filter(i -> matched[i])
            .boxed()
            .collect(Collectors.toList());
    }

    public boolean[] matchSampleFilter(Database database, SampleFilter sampleFilter) {
        // TODO This shouldn't be done here?
        //  Validate yearMonthFrom and yearMonthTo
        Pattern pattern = Pattern.compile("(\\d{4})([-]\\d{2})?([-]\\d{2})?");
        if (sampleFilter.getYearMonthFrom() != null) {
            if (!pattern.matcher(sampleFilter.getYearMonthFrom()).matches()) {
                throw new BadRequestException("yearMonthFrom is malformed, it has to be yyyy-mm");
            }
        }
        if (sampleFilter.getYearMonthTo() != null) {
            if (!pattern.matcher(sampleFilter.getYearMonthTo()).matches()) {
                throw new BadRequestException("yearMonthTo is malformed, it has to be yyyy-mm");
            }
        }

        // Filter variant
        var nucMutations = sampleFilter.getNucMutations();
        var useNucMutations = nucMutations != null && !nucMutations.isEmpty();
        var aaMutations = sampleFilter.getAaMutations();
        var useAaMutations = aaMutations != null && !aaMutations.isEmpty();
        var nucInsertions = sampleFilter.getNucInsertions();
        var useNucInsertions = nucInsertions != null && !nucInsertions.isEmpty();
        var aaInsertions = sampleFilter.getAaInsertions();
        var useAaInsertions = aaInsertions != null && !aaInsertions.isEmpty();
        var pangoLineage = sampleFilter.getPangoLineage();
        var usePangoLineage = pangoLineage != null;
        var nextcladePangoLineage = sampleFilter.getNextcladePangoLineage();
        var useNextcladePangoLineage = nextcladePangoLineage != null;
        var gisaidClade = sampleFilter.getGisaidClade();
        var useGisaidClade = gisaidClade != null;
        var nextstrainClade = sampleFilter.getNextstrainClade();
        var useNextstrainClade = nextstrainClade != null;
        var variantQuery = sampleFilter.getVariantQuery();
        var useVariantQuery = variantQuery != null;

        boolean useOtherVariantSpecifying = useNucMutations || useAaMutations || useNucInsertions || useAaInsertions
            || usePangoLineage || useNextcladePangoLineage || useGisaidClade || useNextstrainClade;
        if (useVariantQuery && useOtherVariantSpecifying) {
            throw new RuntimeException("It is not allowed to use variantQuery and another variant-specifying " +
                "field at the same time.");
        }

        QueryExpr queryExpr = null;
        if (useVariantQuery) {
            queryExpr = parseVariantQueryExpr(variantQuery);
        } else if (useOtherVariantSpecifying) {
            List<QueryExpr> components = new ArrayList<>();
            if (usePangoLineage) {
                PangoQuery pq;
                if (pangoLineage.endsWith("*")) {
                    pq = new PangoQuery(pangoLineage.substring(0, pangoLineage.length() - 1), true,
                        Database.Columns.PANGO_LINEAGE);
                } else {
                    pq = new PangoQuery(pangoLineage, false, Database.Columns.PANGO_LINEAGE);
                }
                components.add(pq);
            }
            if (useNextcladePangoLineage) {
                PangoQuery pq;
                if (nextcladePangoLineage.endsWith("*")) {
                    pq = new PangoQuery(nextcladePangoLineage.substring(0, nextcladePangoLineage.length() - 1), true,
                        NEXTCLADE_PANGO_LINEAGE);
                } else {
                    pq = new PangoQuery(nextcladePangoLineage, false, NEXTCLADE_PANGO_LINEAGE);
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
            if (useNucInsertions) {
                components.addAll(nucInsertions);
            }
            if (useAaInsertions) {
                components.addAll(aaInsertions);
            }
            queryExpr = components.get(0);
            for (int i = 1; i < components.size(); i++) {
                BiOp conjunction = new BiOp(BiOp.OpType.AND);
                conjunction.putValue(queryExpr);
                conjunction.putValue(components.get(i));
                queryExpr = conjunction;
            }
        }

        int numberRows = database.size();
        boolean[] matched;
        if (queryExpr != null) {
            Maybe.pushDownMaybe(queryExpr);
            matched = queryExpr.evaluate(database);
        } else {
            matched = new boolean[numberRows];
            Arrays.fill(matched, true);
        }

        // Filter metadata
        between(matched, database.getIntColumn(DATE), sampleFilter.getDateFrom(), sampleFilter.getDateTo());
        between(matched, database.getIntColumn(YEAR), sampleFilter.getYearFrom(), sampleFilter.getYearTo());
        betweenYearMonth(matched, database.getIntColumn(YEAR), database.getIntColumn(MONTH),
            sampleFilter.getYearMonthFrom(), sampleFilter.getYearMonthTo());
        between(matched, database.getIntColumn(DATE_SUBMITTED),
            sampleFilter.getDateSubmittedFrom(), sampleFilter.getDateSubmittedTo());
        eq(matched, database.getStringColumn(REGION), sampleFilter.getRegion(), true);
        eq(matched, database.getStringColumn(COUNTRY), sampleFilter.getCountry(), true);
        eq(matched, database.getStringColumn(DIVISION), sampleFilter.getDivision(), true);
        eq(matched, database.getStringColumn(LOCATION), sampleFilter.getLocation(), true);
        eq(matched, database.getStringColumn(REGION_EXPOSURE), sampleFilter.getRegionExposure(), true);
        eq(matched, database.getStringColumn(COUNTRY_EXPOSURE), sampleFilter.getCountryExposure(), true);
        eq(matched, database.getStringColumn(DIVISION_EXPOSURE), sampleFilter.getDivisionExposure(), true);
        between(matched, database.getIntColumn(AGE), sampleFilter.getAgeFrom(), sampleFilter.getAgeTo());
        eq(matched, database.getStringColumn(SEX), sampleFilter.getSex(), true);
        eq(matched, database.getBoolColumn(HOSPITALIZED), sampleFilter.getHospitalized());
        eq(matched, database.getBoolColumn(DIED), sampleFilter.getDied());
        eq(matched, database.getBoolColumn(FULLY_VACCINATED), sampleFilter.getFullyVaccinated());
        eq(matched, database.getStringColumn(HOST), sampleFilter.getHost(), true);
        eq(matched, database.getStringColumn(SAMPLING_STRATEGY), sampleFilter.getSamplingStrategy(), true);
        eq(matched, database.getStringColumn(SUBMITTING_LAB), sampleFilter.getSubmittingLab(), true);
        eq(matched, database.getStringColumn(ORIGINATING_LAB), sampleFilter.getOriginatingLab(), true);
        between(matched, database.getFloatColumn(NEXTCLADE_QC_OVERALL_SCORE),
            sampleFilter.getNextcladeQcOverallScoreFrom(), sampleFilter.getNextcladeQcOverallScoreTo());
        between(matched, database.getFloatColumn(NEXTCLADE_QC_MISSING_DATA_SCORE),
            sampleFilter.getNextcladeQcMissingDataScoreFrom(), sampleFilter.getNextcladeQcMissingDataScoreTo());
        between(matched, database.getFloatColumn(NEXTCLADE_QC_MIXED_SITES_SCORE),
            sampleFilter.getNextcladeQcMixedSitesScoreFrom(), sampleFilter.getNextcladeQcMixedSitesScoreTo());
        between(matched, database.getFloatColumn(NEXTCLADE_QC_PRIVATE_MUTATIONS_SCORE),
            sampleFilter.getNextcladeQcPrivateMutationsScoreFrom(), sampleFilter.getNextcladeQcPrivateMutationsScoreTo());
        between(matched, database.getFloatColumn(NEXTCLADE_QC_SNP_CLUSTERS_SCORE),
            sampleFilter.getNextcladeQcSnpClustersScoreFrom(), sampleFilter.getNextcladeQcSnpClustersScoreTo());
        between(matched, database.getFloatColumn(NEXTCLADE_QC_FRAME_SHIFTS_SCORE),
            sampleFilter.getNextcladeQcFrameShiftsScoreFrom(), sampleFilter.getNextcladeQcFrameShiftsScoreTo());
        between(matched, database.getFloatColumn(NEXTCLADE_QC_STOP_CODONS_SCORE),
            sampleFilter.getNextcladeQcStopCodonsScoreFrom(), sampleFilter.getNextcladeQcStopCodonsScoreTo());
        between(matched, database.getFloatColumn(NEXTCLADE_COVERAGE),
            sampleFilter.getNextcladeCoverageFrom(), sampleFilter.getNextcladeCoverageTo());

        // Filter IDs
        eq(matched, database.getStringColumn(GENBANK_ACCESSION), sampleFilter.getGenbankAccession(), true);
        eq(matched, database.getStringColumn(SRA_ACCESSION), sampleFilter.getSraAccession(), true);
        eq(matched, database.getStringColumn(GISAID_EPI_ISL), sampleFilter.getGisaidEpiIsl(), true);
        eq(matched, database.getStringColumn(STRAIN), sampleFilter.getStrain(), true);

        return matched;
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

    private void eq(boolean[] matched, String[] data, Collection<String> possibleValues, boolean caseSensitive) {
        if (possibleValues == null || possibleValues.isEmpty()) {
            return;
        }
        Set<String> possibleValuesSet;
        if (caseSensitive) {
            possibleValuesSet = new HashSet<>(possibleValues);
        } else {
            // Turning everything to lower case
            possibleValuesSet = possibleValues.stream().map(String::toLowerCase).collect(Collectors.toSet());
        }

        if (caseSensitive) {
            for (int i = 0; i < matched.length; i++) {
                matched[i] = matched[i] && possibleValuesSet.contains(data[i]);
            }
        } else {
            for (int i = 0; i < matched.length; i++) {
                matched[i] = matched[i] && possibleValuesSet.contains(data[i] != null ? data[i].toLowerCase() : null);
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

    private <N extends Number & Comparable<N>> void between(boolean[] matched, N[] data, N from, N to) {
        if (from == null && to == null) {
            return;
        }
        if (from != null && to != null) {
            for (int i = 0; i < matched.length; i++) {
                matched[i] =
                    matched[i] && data[i] != null && data[i].compareTo(from) >= 0 && data[i].compareTo(to) <= 0;
            }
        } else if (from != null) {
            for (int i = 0; i < matched.length; i++) {
                matched[i] = matched[i] && data[i] != null && data[i].compareTo(from) >= 0;
            }
        } else {
            for (int i = 0; i < matched.length; i++) {
                matched[i] = matched[i] && data[i] != null && data[i].compareTo(to) <= 0;
            }
        }
    }

    private void between(boolean[] matched, Integer[] data, LocalDate dateFrom, LocalDate dateTo) {
        Integer dateFromInt = Database.dateToInt(dateFrom);
        Integer dateToInt = Database.dateToInt(dateTo);
        between(matched, data, dateFromInt, dateToInt);
    }


    private void betweenYearMonth(
        boolean[] matched,
        Integer[] years,
        Integer[] months,
        String yearMonthFrom,
        String yearMonthTo
    ) {
        if (yearMonthFrom == null && yearMonthTo == null) {
            return;
        }
        Integer yearFromInt = null;
        Integer monthFromInt = null;
        Integer yearToInt = null;
        Integer monthToInt = null;
        if (yearMonthFrom != null) {
            String[] split = yearMonthFrom.split("-");
            yearFromInt = Utils.nullableIntegerValue(split[0]);
            monthFromInt = Utils.nullableIntegerValue(split[1]);
        }
        if (yearMonthTo != null) {
            String[] split = yearMonthTo.split("-");
            yearToInt = Utils.nullableIntegerValue(split[0]);
            monthToInt = Utils.nullableIntegerValue(split[1]);
        }
        if (yearMonthFrom != null && yearMonthTo != null) {
            for (int i = 0; i < matched.length; i++) {
                matched[i] = matched[i] && years[i] != null && months[i] != null
                    && ((years[i] >= yearFromInt && months[i] >= monthFromInt) || years[i] > yearFromInt)
                    && ((years[i] <= yearToInt && months[i] <= monthToInt) || years[i] < yearToInt);
            }
        } else if (yearMonthFrom != null) {
            for (int i = 0; i < matched.length; i++) {
                matched[i] = matched[i] && years[i] != null && months[i] != null
                    && ((years[i] >= yearFromInt && months[i] >= monthFromInt) || years[i] > yearFromInt);
            }
        } else {
            for (int i = 0; i < matched.length; i++) {
                matched[i] = matched[i] && years[i] != null && months[i] != null
                    && ((years[i] <= yearToInt && months[i] <= monthToInt) || years[i] < yearToInt);
            }
        }
    }

    private QueryExpr parseVariantQueryExpr(String variantQuery) {
        try {
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
        } catch (ParseCancellationException e) {
            log.error("Malformed variant query: " +
                variantQuery.substring(0, Math.min(200, variantQuery.length())));
            throw new MalformedVariantQueryException();
        }
    }

    public static String aggregationFieldToColumnName(AggregationField field) {
        return switch (field) {
            case DATE -> DATE;
            case YEAR -> YEAR;
            case MONTH -> MONTH;
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
            case NEXTCLADEPANGOLINEAGE -> NEXTCLADE_PANGO_LINEAGE;
            case NEXTSTRAINCLADE -> NEXTSTRAIN_CLADE;
            case GISAIDCLADE -> GISAID_CLADE;
            case SUBMITTINGLAB -> SUBMITTING_LAB;
            case ORIGINATINGLAB -> ORIGINATING_LAB;
            case DATABASE -> DATABASE;
        };
    }

    public static AggregationField columnNameToaggregationField(String column) {
        return switch (column) {
            case DATE -> AggregationField.DATE;
            case YEAR -> AggregationField.YEAR;
            case MONTH -> AggregationField.MONTH;
            case DATE_SUBMITTED -> AggregationField.DATESUBMITTED;
            case REGION -> AggregationField.REGION;
            case COUNTRY -> AggregationField.COUNTRY;
            case DIVISION -> AggregationField.DIVISION;
            case LOCATION -> AggregationField.LOCATION;
            case REGION_EXPOSURE -> AggregationField.REGIONEXPOSURE;
            case COUNTRY_EXPOSURE -> AggregationField.COUNTRYEXPOSURE;
            case DIVISION_EXPOSURE -> AggregationField.DIVISIONEXPOSURE;
            case AGE -> AggregationField.AGE;
            case SEX -> AggregationField.SEX;
            case HOSPITALIZED -> AggregationField.HOSPITALIZED;
            case DIED -> AggregationField.DIED;
            case FULLY_VACCINATED -> AggregationField.FULLYVACCINATED;
            case HOST -> AggregationField.HOST;
            case SAMPLING_STRATEGY -> AggregationField.SAMPLINGSTRATEGY;
            case PANGO_LINEAGE -> AggregationField.PANGOLINEAGE;
            case NEXTCLADE_PANGO_LINEAGE -> AggregationField.NEXTCLADEPANGOLINEAGE;
            case NEXTSTRAIN_CLADE -> AggregationField.NEXTSTRAINCLADE;
            case GISAID_CLADE -> AggregationField.GISAIDCLADE;
            case SUBMITTING_LAB -> AggregationField.SUBMITTINGLAB;
            case ORIGINATING_LAB -> AggregationField.ORIGINATINGLAB;
            case DATABASE -> AggregationField.DATABASE;
            default -> throw new IllegalStateException("Unexpected value: " + column);
        };
    }

}
