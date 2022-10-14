package ch.ethz.lapis.api.query;

import static ch.ethz.lapis.api.query.Database.Columns.AGE;
import static ch.ethz.lapis.api.query.Database.Columns.COUNTRY;
import static ch.ethz.lapis.api.query.Database.Columns.COUNTRY_EXPOSURE;
import static ch.ethz.lapis.api.query.Database.Columns.DATE;
import static ch.ethz.lapis.api.query.Database.Columns.DATE_SUBMITTED;
import static ch.ethz.lapis.api.query.Database.Columns.DIED;
import static ch.ethz.lapis.api.query.Database.Columns.DIVISION;
import static ch.ethz.lapis.api.query.Database.Columns.DIVISION_EXPOSURE;
import static ch.ethz.lapis.api.query.Database.Columns.FULLY_VACCINATED;
import static ch.ethz.lapis.api.query.Database.Columns.GENBANK_ACCESSION;
import static ch.ethz.lapis.api.query.Database.Columns.GISAID_CLADE;
import static ch.ethz.lapis.api.query.Database.Columns.GISAID_EPI_ISL;
import static ch.ethz.lapis.api.query.Database.Columns.HOSPITALIZED;
import static ch.ethz.lapis.api.query.Database.Columns.HOST;
import static ch.ethz.lapis.api.query.Database.Columns.LOCATION;
import static ch.ethz.lapis.api.query.Database.Columns.MONTH;
import static ch.ethz.lapis.api.query.Database.Columns.NEXTCLADE_COVERAGE;
import static ch.ethz.lapis.api.query.Database.Columns.NEXTCLADE_PANGO_LINEAGE;
import static ch.ethz.lapis.api.query.Database.Columns.NEXTCLADE_QC_FRAME_SHIFTS_SCORE;
import static ch.ethz.lapis.api.query.Database.Columns.NEXTCLADE_QC_MISSING_DATA_SCORE;
import static ch.ethz.lapis.api.query.Database.Columns.NEXTCLADE_QC_MIXED_SITES_SCORE;
import static ch.ethz.lapis.api.query.Database.Columns.NEXTCLADE_QC_OVERALL_SCORE;
import static ch.ethz.lapis.api.query.Database.Columns.NEXTCLADE_QC_PRIVATE_MUTATIONS_SCORE;
import static ch.ethz.lapis.api.query.Database.Columns.NEXTCLADE_QC_SNP_CLUSTERS_SCORE;
import static ch.ethz.lapis.api.query.Database.Columns.NEXTCLADE_QC_STOP_CODONS_SCORE;
import static ch.ethz.lapis.api.query.Database.Columns.NEXTSTRAIN_CLADE;
import static ch.ethz.lapis.api.query.Database.Columns.ORIGINATING_LAB;
import static ch.ethz.lapis.api.query.Database.Columns.PANGO_LINEAGE;
import static ch.ethz.lapis.api.query.Database.Columns.REGION;
import static ch.ethz.lapis.api.query.Database.Columns.REGION_EXPOSURE;
import static ch.ethz.lapis.api.query.Database.Columns.SAMPLING_STRATEGY;
import static ch.ethz.lapis.api.query.Database.Columns.SEX;
import static ch.ethz.lapis.api.query.Database.Columns.SRA_ACCESSION;
import static ch.ethz.lapis.api.query.Database.Columns.STRAIN;
import static ch.ethz.lapis.api.query.Database.Columns.SUBMITTING_LAB;
import static ch.ethz.lapis.api.query.Database.Columns.YEAR;

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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

public class QueryEngine {

    public List<SampleAggregated> aggregate(Database database, SampleAggregatedRequest request) {
        // Filter
        boolean[] matched = matchSampleFilter(database, request);
        int numberRows = database.size();

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
                    }
                }
                result.add(sampleAggregated);
            }
        }

        return result;
    }

    public List<Integer> filterIds(Database database, SampleFilter<?> sampleFilter) {
        boolean[] matched = matchSampleFilter(database, sampleFilter);
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < matched.length; i++) {
            if (matched[i]) {
                ids.add(i);
            }
        }
        return ids;
    }

    public List<Integer> filterIdsReversed(Database database, SampleFilter<?> sampleFilter) {
        boolean[] matched = matchSampleFilter(database, sampleFilter);
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < matched.length; i++) {
            if (!matched[i]) {
                ids.add(i);
            }
        }
        return ids;
    }

    public boolean[] matchSampleFilter(Database database, SampleFilter<?> sampleFilter) {
        Database db = database;
        SampleFilter<?> sf = sampleFilter;

        // TODO This shouldn't be done here?
        //  Validate yearMonthFrom and yearMonthTo
        Pattern pattern = Pattern.compile("(\\d{4})([-]\\d{2})?([-]\\d{2})?");
        if (sf.getYearMonthFrom() != null) {
            if (!pattern.matcher(sf.getYearMonthFrom()).matches()) {
                throw new BadRequestException("yearMonthFrom is malformed, it has to be yyyy-mm");
            }
        }
        if (sf.getYearMonthTo() != null) {
            if (!pattern.matcher(sf.getYearMonthTo()).matches()) {
                throw new BadRequestException("yearMonthTo is malformed, it has to be yyyy-mm");
            }
        }

        // Filter variant
        var nucMutations = sf.getNucMutations();
        var useNucMutations = nucMutations != null && !nucMutations.isEmpty();
        var aaMutations = sf.getAaMutations();
        var useAaMutations = aaMutations != null && !aaMutations.isEmpty();
        var nucInsertions = sf.getNucInsertions();
        var useNucInsertions = nucInsertions != null && !nucInsertions.isEmpty();
        var aaInsertions = sf.getAaInsertions();
        var useAaInsertions = aaInsertions != null && !aaInsertions.isEmpty();
        var pangoLineage = sf.getPangoLineage();
        var usePangoLineage = pangoLineage != null;
        var nextcladePangoLineage = sf.getNextcladePangoLineage();
        var useNextcladePangoLineage = nextcladePangoLineage != null;
        var gisaidClade = sf.getGisaidClade();
        var useGisaidClade = gisaidClade != null;
        var nextstrainClade = sf.getNextstrainClade();
        var useNextstrainClade = nextstrainClade != null;
        var variantQuery = sf.getVariantQuery();
        var useVariantQuery = variantQuery != null;

        boolean useOtherVariantSpecifying = useNucMutations || useAaMutations || useNucInsertions || useAaInsertions
            || usePangoLineage || useNextcladePangoLineage || useGisaidClade || useNextstrainClade;
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
            variantQueryExpr = components.get(0);
            for (int i = 1; i < components.size(); i++) {
                BiOp conjunction = new BiOp(BiOp.OpType.AND);
                conjunction.putValue(variantQueryExpr);
                conjunction.putValue(components.get(i));
                variantQueryExpr = conjunction;
            }
        }

        int numberRows = db.size();
        boolean[] matched;
        if (variantQueryExpr != null) {
            Maybe.pushDownMaybe(variantQueryExpr);
            matched = variantQueryExpr.evaluate(db);
        } else {
            matched = new boolean[numberRows];
            Arrays.fill(matched, true);
        }

        // Filter metadata
        between(matched, db.getIntColumn(DATE), sf.getDateFrom(), sf.getDateTo());
        between(matched, db.getIntColumn(YEAR), sf.getYearFrom(), sf.getYearTo());
        betweenYearMonth(matched, db.getIntColumn(YEAR), db.getIntColumn(MONTH),
            sf.getYearMonthFrom(), sf.getYearMonthTo());
        between(matched, db.getIntColumn(DATE_SUBMITTED),
            sf.getDateSubmittedFrom(), sf.getDateSubmittedTo());
        eq(matched, db.getStringColumn(REGION), sf.getRegion(), true);
        eq(matched, db.getStringColumn(COUNTRY), sf.getCountry(), true);
        eq(matched, db.getStringColumn(DIVISION), sf.getDivision(), true);
        eq(matched, db.getStringColumn(LOCATION), sf.getLocation(), true);
        eq(matched, db.getStringColumn(REGION_EXPOSURE), sf.getRegionExposure(), true);
        eq(matched, db.getStringColumn(COUNTRY_EXPOSURE), sf.getCountryExposure(), true);
        eq(matched, db.getStringColumn(DIVISION_EXPOSURE), sf.getDivisionExposure(), true);
        between(matched, db.getIntColumn(AGE), sf.getAgeFrom(), sf.getAgeTo());
        eq(matched, db.getStringColumn(SEX), sf.getSex(), true);
        eq(matched, db.getBoolColumn(HOSPITALIZED), sf.getHospitalized());
        eq(matched, db.getBoolColumn(DIED), sf.getDied());
        eq(matched, db.getBoolColumn(FULLY_VACCINATED), sf.getFullyVaccinated());
        eq(matched, db.getStringColumn(HOST), sf.getHost(), true);
        eq(matched, db.getStringColumn(SAMPLING_STRATEGY), sf.getSamplingStrategy(), true);
        eq(matched, db.getStringColumn(SUBMITTING_LAB), sf.getSubmittingLab(), true);
        eq(matched, db.getStringColumn(ORIGINATING_LAB), sf.getOriginatingLab(), true);
        between(matched, db.getFloatColumn(NEXTCLADE_QC_OVERALL_SCORE),
            sf.getNextcladeQcOverallScoreFrom(), sf.getNextcladeQcOverallScoreTo());
        between(matched, db.getFloatColumn(NEXTCLADE_QC_MISSING_DATA_SCORE),
            sf.getNextcladeQcMissingDataScoreFrom(), sf.getNextcladeQcMissingDataScoreTo());
        between(matched, db.getFloatColumn(NEXTCLADE_QC_MIXED_SITES_SCORE),
            sf.getNextcladeQcMixedSitesScoreFrom(), sf.getNextcladeQcMixedSitesScoreTo());
        between(matched, db.getFloatColumn(NEXTCLADE_QC_PRIVATE_MUTATIONS_SCORE),
            sf.getNextcladeQcPrivateMutationsScoreFrom(), sf.getNextcladeQcPrivateMutationsScoreTo());
        between(matched, db.getFloatColumn(NEXTCLADE_QC_SNP_CLUSTERS_SCORE),
            sf.getNextcladeQcSnpClustersScoreFrom(), sf.getNextcladeQcSnpClustersScoreTo());
        between(matched, db.getFloatColumn(NEXTCLADE_QC_FRAME_SHIFTS_SCORE),
            sf.getNextcladeQcFrameShiftsScoreFrom(), sf.getNextcladeQcFrameShiftsScoreTo());
        between(matched, db.getFloatColumn(NEXTCLADE_QC_STOP_CODONS_SCORE),
            sf.getNextcladeQcStopCodonsScoreFrom(), sf.getNextcladeQcStopCodonsScoreTo());
        between(matched, db.getFloatColumn(NEXTCLADE_COVERAGE),
            sf.getNextcladeCoverageFrom(), sf.getNextcladeCoverageTo());

        // Filter IDs
        eq(matched, db.getStringColumn(GENBANK_ACCESSION), sf.getGenbankAccession(), true);
        eq(matched, db.getStringColumn(SRA_ACCESSION), sf.getSraAccession(), true);
        eq(matched, db.getStringColumn(GISAID_EPI_ISL), sf.getGisaidEpiIsl(), true);
        eq(matched, db.getStringColumn(STRAIN), sf.getStrain(), true);

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

    private void between(boolean[] matched, Float[] data, Float from, Float to) {
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

    private VariantQueryExpr parseVariantQueryExpr(String variantQuery) {
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
            System.err.println("Malformed variant query: " +
                variantQuery.substring(0, Math.min(200, variantQuery.length())));
            throw new MalformedVariantQueryException();
        }
    }

    private String aggregationFieldToColumnName(AggregationField field) {
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
            default -> throw new IllegalStateException("Unexpected value: " + field);
        };
    }

}
