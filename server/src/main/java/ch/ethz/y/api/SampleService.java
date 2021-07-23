package ch.ethz.y.api;

import ch.ethz.y.YCoVDBMain;
import ch.ethz.y.api.entity.AAMutation;
import ch.ethz.y.api.entity.AggregationField;
import ch.ethz.y.api.entity.NucMutation;
import ch.ethz.y.api.entity.req.SampleAggregatedRequest;
import ch.ethz.y.api.entity.req.SampleDetailRequest;
import ch.ethz.y.api.entity.req.SampleFilter;
import ch.ethz.y.api.entity.res.SampleAggregated;
import ch.ethz.y.api.entity.res.SampleDetail;
import ch.ethz.y.core.DatabaseService;
import ch.ethz.y.util.DeflateSeqCompressor;
import ch.ethz.y.util.PangolinLineageAlias;
import ch.ethz.y.util.PangolinLineageAliasResolver;
import ch.ethz.y.util.SeqCompressor;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.jooq.Record;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.y.tables.YMainAaSequenceColumnar;
import org.jooq.y.tables.YMainMetadata;
import org.jooq.y.tables.YMainSequence;
import org.jooq.y.tables.records.YMainSequenceRecord;
import org.springframework.stereotype.Service;

import java.sql.Statement;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SampleService {

    private static final ComboPooledDataSource dbPool
            = DatabaseService.createDatabaseConnectionPool(YCoVDBMain.globalConfig.getVineyard());
    private static final SeqCompressor referenceSeqCompressor
            = new DeflateSeqCompressor(DeflateSeqCompressor.DICT.REFERENCE);
    private static final SeqCompressor nucMutationColumnarCompressor
            = new DeflateSeqCompressor(DeflateSeqCompressor.DICT.ATCGNDEL);
    private static final SeqCompressor aaMutationColumnarCompressor
            = new DeflateSeqCompressor(DeflateSeqCompressor.DICT.AACODONS);
    private final PangolinLineageAliasResolver pangolinLineageAliasResolver;


    public SampleService() {
        try {
            // TODO This will be only loaded once and will not reload when the aliases change. The aliases should not
            //   change too often so it is not a very big issue but it could potentially cause unexpected results.
            this.pangolinLineageAliasResolver = new PangolinLineageAliasResolver(getPangolinLineageAliases());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    private Connection getDatabaseConnection() throws SQLException {
        return dbPool.getConnection();
    }


    public List<SampleAggregated> getAggregatedSamples(SampleAggregatedRequest request) throws SQLException {
        List<AggregationField> fields = request.getFields();

        // Filter the IDs by nucleotide mutations (if requested)
        List<Integer> nucIds = null;
        if (request.getNucMutations() != null && !request.getNucMutations().isEmpty()) {
            nucIds = getIdsWithNucMutations(request.getNucMutations());
            System.out.println("I found " + nucIds.size() + " with the searched " + request.getNucMutations().size()
                    + " nucleotide mutations.");
        }
        if (nucIds != null && nucIds.isEmpty()) {
            return new ArrayList<>();
        }

        // Filter the IDs by amino acid mutations (if requested)
        List<Integer> aaIds = null;
        if (request.getAaMutations() != null && !request.getAaMutations().isEmpty()) {
            aaIds = getIdsWithAAMutations(request.getAaMutations());
            System.out.println("I found " + aaIds.size() + " with the searched " + request.getAaMutations().size()
                    + " amino acid mutations.");
        }

        // Merge the nuc and aa mutation filter results
        Set<Integer> ids = null;
        if (nucIds != null && aaIds != null) {
            ids = new HashSet<>(nucIds);
            ids.retainAll(new HashSet<>(aaIds));
        } else if (nucIds != null) {
            ids = new HashSet<>(nucIds);
        } else if (aaIds != null) {
            ids = new HashSet<>(aaIds);
        }
        if (ids != null) {
            System.out.println("There are " + ids.size() + " with all the searched nucleotide and amino acid " +
                    "mutations");
        }
        if (ids != null && ids.isEmpty()) {
            return new ArrayList<>();
        }

        // Filter further by the other metadata and prepare the response
        List<SampleAggregated> samples = new ArrayList<>();
        try (Connection conn = getDatabaseConnection()) {
            DSLContext ctx = JooqHelper.getDSLCtx(conn);
            YMainMetadata tbl = YMainMetadata.Y_MAIN_METADATA;

            List<TableField<?, ?>> groupByFields = getTableFields(fields, tbl);
            List<Field<?>> selectFields = new ArrayList<>(groupByFields);
            selectFields.add(DSL.count().as("count"));
            List<Condition> conditions = getConditions(request, tbl);

            Result<Record> records;
            if (ids != null) {
                Table<Record1<Object>> idsTbl = DSL.values(ids.stream()
                        .map(DSL::row)
                        .collect(Collectors.toList())
                        .toArray(new Row1[0])
                ).as("ids", "id");
                var statement = ctx
                        .select(selectFields)
                        .from(idsTbl.join(tbl).on(idsTbl.field("id", Integer.class).eq(tbl.ID)))
                        .where(conditions)
                        .groupBy(groupByFields);
                records = statement.fetch();
            } else {
                var statement = ctx
                        .select(selectFields)
                        .from(tbl)
                        .where(conditions)
                        .groupBy(groupByFields);
                records = statement.fetch();
            }
            for (var r : records) {
                SampleAggregated sample = new SampleAggregated()
                        .setCount(r.get("count", Integer.class));
                if (fields.contains(AggregationField.DATE)) {
                    sample.setDate(r.get(tbl.DATE));
                }
                if (fields.contains(AggregationField.DATESUBMITTED)) {
                    sample.setDateSubmitted(r.get(tbl.DATE_SUBMITTED));
                }
                if (fields.contains(AggregationField.REGION)) {
                    sample.setRegion(r.get(tbl.REGION));
                }
                if (fields.contains(AggregationField.COUNTRY)) {
                    sample.setCountry(r.get(tbl.COUNTRY));
                }
                if (fields.contains(AggregationField.DIVISION)) {
                    sample.setDivision(r.get(tbl.DIVISION));
                }
                if (fields.contains(AggregationField.LOCATION)) {
                    sample.setLocation(r.get(tbl.LOCATION));
                }
                if (fields.contains(AggregationField.REGIONEXPOSURE)) {
                    sample.setRegionExposure(r.get(tbl.REGION_EXPOSURE));
                }
                if (fields.contains(AggregationField.COUNTRYEXPOSURE)) {
                    sample.setCountryExposure(r.get(tbl.COUNTRY_EXPOSURE));
                }
                if (fields.contains(AggregationField.DIVISIONEXPOSURE)) {
                    sample.setDivisionExposure(r.get(tbl.DIVISION_EXPOSURE));
                }
                if (fields.contains(AggregationField.AGE)) {
                    sample.setAge(r.get(tbl.AGE));
                }
                if (fields.contains(AggregationField.SEX)) {
                    sample.setSex(r.get(tbl.SEX));
                }
                if (fields.contains(AggregationField.HOST)) {
                    sample.setHost(r.get(tbl.HOST));
                }
                if (fields.contains(AggregationField.SAMPLINGSTRATEGY)) {
                    sample.setSamplingStrategy(r.get(tbl.SAMPLING_STRATEGY));
                }
                if (fields.contains(AggregationField.PANGOLINEAGE)) {
                    sample.setPangoLineage(r.get(tbl.PANGO_LINEAGE));
                }
                if (fields.contains(AggregationField.NEXTSTRAINCLADE)) {
                    sample.setNextstrainClade(r.get(tbl.NEXTSTRAIN_CLADE));
                }
                if (fields.contains(AggregationField.GISAIDCLADE)) {
                    sample.setGisaidCloade(r.get(tbl.GISAID_CLADE));
                }
                if (fields.contains(AggregationField.SUBMITTINGLAB)) {
                    sample.setSubmittingLab(r.get(tbl.SUBMITTING_LAB));
                }
                if (fields.contains(AggregationField.ORIGINATINGLAB)) {
                    sample.setOriginatingLab(r.get(tbl.ORIGINATING_LAB));
                }
                samples.add(sample);
            }
        }
        return samples;
    }


    public List<SampleDetail> getDetailedSamples(SampleDetailRequest request) throws SQLException {
        // Filter the IDs by mutations (if requested)
        List<Integer> ids = null;
        if (request.getNucMutations() != null && !request.getNucMutations().isEmpty()) {
            ids = getIdsWithNucMutations(request.getNucMutations());
            System.out.println("I found " + ids.size() + " with the searched " + request.getNucMutations().size()
                    + " nucleotide mutations.");
        }

        // Filter further by the other metadata and prepare the response
        List<SampleDetail> samples = new ArrayList<>();
        try (Connection conn = getDatabaseConnection()) {
            DSLContext ctx = JooqHelper.getDSLCtx(conn);
            YMainMetadata tbl = YMainMetadata.Y_MAIN_METADATA;

            List<Field<?>> selectFields = new ArrayList<>() {{
                add(tbl.GENBANK_ACCESSION);
                add(tbl.SRA_ACCESSION);
                add(tbl.GISAID_EPI_ISL);
                add(tbl.DATE);
                add(tbl.DATE_SUBMITTED);
                add(tbl.REGION);
                add(tbl.COUNTRY);
                add(tbl.DIVISION);
                add(tbl.LOCATION);
                add(tbl.REGION_EXPOSURE);
                add(tbl.COUNTRY_EXPOSURE);
                add(tbl.DIVISION_EXPOSURE);;
                add(tbl.AGE);
                add(tbl.SEX);
                add(tbl.HOST);
                add(tbl.SAMPLING_STRATEGY);
                add(tbl.PANGO_LINEAGE);
                add(tbl.NEXTSTRAIN_CLADE);
                add(tbl.GISAID_CLADE);
                add(tbl.SUBMITTING_LAB);
                add(tbl.ORIGINATING_LAB);
                add(tbl.AUTHORS);
            }};
            List<Condition> conditions = getConditions(request, tbl);

            Result<Record> records;
            if (ids != null) {
                Table<Record1<Object>> idsTbl = DSL.values(ids.stream()
                        .map(DSL::row)
                        .collect(Collectors.toList())
                        .toArray(new Row1[0])
                ).as("ids", "id");
                var statement = ctx
                        .select(selectFields)
                        .from(idsTbl.join(tbl).on(idsTbl.field("id", Integer.class).eq(tbl.ID)))
                        .where(conditions);
                records = statement.fetch();
            } else {
                var statement = ctx
                        .select(selectFields)
                        .from(tbl)
                        .where(conditions);
                records = statement.fetch();
            }
            for (var r : records) {
                SampleDetail sample = new SampleDetail()
                        .setGenbankAccession(r.get(tbl.GENBANK_ACCESSION))
                        .setSraAccession(r.get(tbl.SRA_ACCESSION))
                        .setGisaidEpiIsl(r.get(tbl.GISAID_EPI_ISL))
                        .setDate(r.get(tbl.DATE))
                        .setDateSubmitted(r.get(tbl.DATE_SUBMITTED))
                        .setRegion(r.get(tbl.REGION))
                        .setCountry(r.get(tbl.COUNTRY))
                        .setDivision(r.get(tbl.DIVISION))
                        .setLocation(r.get(tbl.LOCATION))
                        .setRegionExposure(r.get(tbl.REGION_EXPOSURE))
                        .setCountryExposure(r.get(tbl.COUNTRY_EXPOSURE))
                        .setDivisionExposure(r.get(tbl.DIVISION_EXPOSURE))
                        .setAge(r.get(tbl.AGE))
                        .setSex(r.get(tbl.SEX))
                        .setHost(r.get(tbl.HOST))
                        .setSamplingStrategy(r.get(tbl.SAMPLING_STRATEGY))
                        .setPangoLineage(r.get(tbl.PANGO_LINEAGE))
                        .setNextstrainClade(r.get(tbl.NEXTSTRAIN_CLADE))
                        .setGisaidCloade(r.get(tbl.GISAID_CLADE))
                        .setSubmittingLab(r.get(tbl.SUBMITTING_LAB))
                        .setOriginatingLab(r.get(tbl.ORIGINATING_LAB));
                samples.add(sample);
            }
        }
        return samples;
    }


    public String getFasta(SampleDetailRequest request, boolean aligned) throws SQLException {
        // Filter the IDs by mutations (if requested)
        List<Integer> ids = null;
        if (request.getNucMutations() != null && !request.getNucMutations().isEmpty()) {
            ids = getIdsWithNucMutations(request.getNucMutations());
            System.out.println("I found " + ids.size() + " with the searched " + request.getNucMutations().size()
                    + " nucleotide mutations.");
        }

        StringBuilder fastaBuilder = new StringBuilder();

        // Filter further by the other metadata and prepare the response
        List<SampleDetail> samples = new ArrayList<>();
        try (Connection conn = getDatabaseConnection()) {
            DSLContext ctx = JooqHelper.getDSLCtx(conn);
            YMainMetadata metaTbl = YMainMetadata.Y_MAIN_METADATA;
            YMainSequence seqTbl = YMainSequence.Y_MAIN_SEQUENCE;

            TableField<YMainSequenceRecord, byte[]> seqColumn = aligned ?
                    seqTbl.SEQ_ALIGNED_COMPRESSED : seqTbl.SEQ_ORIGINAL_COMPRESSED;

            List<Condition> conditions = getConditions(request, metaTbl);
            Result<Record2<String, byte[]>> records;
            if (ids != null) {
                Table<Record1<Object>> idsTbl = DSL.values(ids.stream()
                        .map(DSL::row)
                        .collect(Collectors.toList())
                        .toArray(new Row1[0])
                ).as("ids", "id");
                var statement = ctx
                        .select(metaTbl.GENBANK_ACCESSION, seqColumn)
                        .from(
                                idsTbl
                                        .join(metaTbl).on(idsTbl.field("id", Integer.class).eq(metaTbl.ID))
                                        .join(seqTbl).on(metaTbl.ID.eq(seqTbl.ID))
                        )
                        .where(conditions)
                        .limit(1000);
                records = statement.fetch();
            } else {
                var statement = ctx
                        .select(metaTbl.GENBANK_ACCESSION, seqColumn)
                        .from(
                                metaTbl
                                        .join(seqTbl).on(metaTbl.ID.eq(seqTbl.ID))
                        )
                        .where(conditions)
                        .limit(1000);
                records = statement.fetch();
            }
            for (var r : records) {
                fastaBuilder
                        .append(">")
                        .append(r.get(metaTbl.GENBANK_ACCESSION))
                        .append("\n")
                        .append(referenceSeqCompressor.decompress(r.get(seqColumn)))
                        .append("\n\n");
            }
        }
        return fastaBuilder.toString();
    }


    private List<Condition> getConditions(SampleFilter<?> request, YMainMetadata tbl) {
        List<Condition> conditions = new ArrayList<>();
        if (request.getDateFrom() != null) {
            conditions.add(tbl.DATE.ge(request.getDateFrom()));
        }
        if (request.getDateTo() != null) {
            conditions.add(tbl.DATE.le(request.getDateTo()));
        }
        if (request.getDateSubmittedFrom() != null) {
            conditions.add(tbl.DATE_SUBMITTED.ge(request.getDateSubmittedFrom()));
        }
        if (request.getDateSubmittedTo() != null) {
            conditions.add(tbl.DATE_SUBMITTED.le(request.getDateSubmittedTo()));
        }
        if (request.getRegion() != null) {
            conditions.add(tbl.REGION.eq(request.getRegion()));
        }
        if (request.getCountry() != null) {
            conditions.add(tbl.COUNTRY.eq(request.getCountry()));
        }
        if (request.getDivision() != null) {
            conditions.add(tbl.DIVISION.eq(request.getDivision()));
        }
        if (request.getLocation() != null) {
            conditions.add(tbl.LOCATION.eq(request.getLocation()));
        }
        if (request.getRegionExposure() != null) {
            conditions.add(tbl.REGION_EXPOSURE.eq(request.getRegionExposure()));
        }
        if (request.getCountryExposure() != null) {
            conditions.add(tbl.COUNTRY_EXPOSURE.eq(request.getCountryExposure()));
        }
        if (request.getDivisionExposure() != null) {
            conditions.add(tbl.DIVISION_EXPOSURE.eq(request.getDivisionExposure()));
        }
        if (request.getAgeFrom() != null) {
            conditions.add(tbl.AGE.ge(request.getAgeFrom()));
        }
        if (request.getAgeTo() != null) {
            conditions.add(tbl.AGE.le(request.getAgeTo()));
        }
        if (request.getSex() != null) {
            conditions.add(tbl.SEX.eq(request.getSex()));
        }
        if (request.getRegion() != null) {
            conditions.add(tbl.REGION.eq(request.getRegion()));
        }
        if (request.getCountry() != null) {
            conditions.add(tbl.COUNTRY.eq(request.getCountry()));
        }
        if (request.getHost() != null) {
            conditions.add(tbl.HOST.eq(request.getHost()));
        }
        String pangoLineage = request.getPangoLineage();
        if (pangoLineage != null) {
            String[] pangolinLineageLikeStatements = parsePangolinLineageQuery(pangoLineage);
            conditions.add(tbl.PANGO_LINEAGE.like(DSL.any(pangolinLineageLikeStatements)));
        }
        if (request.getRegion() != null) {
            conditions.add(tbl.REGION.eq(request.getRegion()));
        }
        if (request.getCountry() != null) {
            conditions.add(tbl.COUNTRY.eq(request.getCountry()));
        }
        if (request.getNextstrainClade() != null) {
            conditions.add(tbl.NEXTSTRAIN_CLADE.eq(request.getNextstrainClade()));
        }
        if (request.getRegion() != null) {
            conditions.add(tbl.REGION.eq(request.getRegion()));
        }
        if (request.getCountry() != null) {
            conditions.add(tbl.COUNTRY.eq(request.getCountry()));
        }
        if (request.getGisaidClade() != null) {
            conditions.add(tbl.GISAID_CLADE.eq(request.getGisaidClade()));
        }
        if (request.getSubmittingLab() != null) {
            conditions.add(tbl.SUBMITTING_LAB.eq(request.getSubmittingLab()));
        }
        if (request.getOriginatingLab() != null) {
            conditions.add(tbl.ORIGINATING_LAB.eq(request.getOriginatingLab()));
        }
        if (request instanceof SampleDetailRequest) {
            SampleDetailRequest sdr = (SampleDetailRequest) request;
            if (sdr.getGenbankAccession() != null) {
                conditions.add(tbl.GENBANK_ACCESSION.eq(sdr.getGenbankAccession()));
            }
            if (sdr.getGisaidEpiIsl() != null) {
                conditions.add(tbl.GISAID_EPI_ISL.eq(sdr.getGisaidEpiIsl()));
            }
        }
        return conditions;
    }


    private List<TableField<?, ?>> getTableFields(List<AggregationField> aggregationFields, YMainMetadata tbl) {
        Map<AggregationField, TableField<?, ?>> ALL_FIELDS = new HashMap<>() {{
            put(AggregationField.DATE, tbl.DATE);
            put(AggregationField.DATESUBMITTED, tbl.DATE_SUBMITTED);
            put(AggregationField.REGION, tbl.REGION);
            put(AggregationField.COUNTRY, tbl.COUNTRY);
            put(AggregationField.DIVISION, tbl.DIVISION);
            put(AggregationField.LOCATION, tbl.LOCATION);
            put(AggregationField.REGIONEXPOSURE, tbl.REGION_EXPOSURE);
            put(AggregationField.COUNTRYEXPOSURE, tbl.COUNTRY_EXPOSURE);
            put(AggregationField.DIVISIONEXPOSURE, tbl.DIVISION_EXPOSURE);
            put(AggregationField.AGE, tbl.AGE);
            put(AggregationField.SEX, tbl.SEX);
            put(AggregationField.HOST, tbl.HOST);
            put(AggregationField.SAMPLINGSTRATEGY, tbl.SAMPLING_STRATEGY);
            put(AggregationField.PANGOLINEAGE, tbl.PANGO_LINEAGE);
            put(AggregationField.NEXTSTRAINCLADE, tbl.NEXTSTRAIN_CLADE);
            put(AggregationField.GISAIDCLADE, tbl.GISAID_CLADE);
            put(AggregationField.SUBMITTINGLAB, tbl.SUBMITTING_LAB);
            put(AggregationField.ORIGINATINGLAB, tbl.ORIGINATING_LAB);
        }};
        return aggregationFields.stream()
                .map(ALL_FIELDS::get)
                .collect(Collectors.toList());
    }


    private List<Integer> getIdsWithNucMutations(List<NucMutation> nucMutations) throws SQLException {
        if (nucMutations == null || nucMutations.isEmpty()) {
            throw new RuntimeException("At least one nucleotide mutation must be provided.");
        }
        Map<Integer, NucMutation> positionToMutation = new HashMap<>();
        List<Integer> positions = new ArrayList<>();
        for (NucMutation nucMutation : nucMutations) {
            positionToMutation.put(nucMutation.getPosition(), nucMutation);
            positions.add(nucMutation.getPosition());
        }
        String sql = """
              select position, data_compressed
              from y_main_sequence_columnar
              where position = any(?::int[]);
        """;
        List<Integer> foundIds = null;
        try (Connection conn = getDatabaseConnection()) {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setArray(1, conn.createArrayOf("int", positions.toArray()));
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        int position = rs.getInt("position");
                        NucMutation searchedMutation = positionToMutation.get(position);
                        byte[] compressed = rs.getBytes("data_compressed");
                        char[] nucleotides = nucMutationColumnarCompressor.decompress(compressed).toCharArray();
                        if (foundIds == null) {
                            // If this is the first round, search all sequences
                            foundIds = new ArrayList<>();
                            for (int i = 0; i < nucleotides.length; i++) {
                                if (isMatchingMutation(nucleotides[i], searchedMutation)) {
                                    foundIds.add(i);
                                }
                            }
                        } else {
                            // In the subsequent rounds, we will just continue filter the foundIds.
                            List<Integer> nextFoundIds = new ArrayList<>();
                            for (Integer foundId : foundIds) {
                                if (isMatchingMutation(nucleotides[foundId], searchedMutation)) {
                                    nextFoundIds.add(foundId);
                                }
                            }
                            foundIds = nextFoundIds;
                        }
                        if (foundIds.isEmpty()) {
                            break;
                        }
                    }
                }
            }
        }
        return foundIds;
    }


    private List<Integer> getIdsWithAAMutations(List<AAMutation> aaMutations) throws SQLException {
        if (aaMutations == null || aaMutations.isEmpty()) {
            throw new RuntimeException("At least one amino acid mutation must be provided.");
        }
        // The gene position will be encoded as e.g., S:501
        Map<String, AAMutation> genePositionToMutation = new HashMap<>();
        for (AAMutation aaMutation : aaMutations) {
            genePositionToMutation.put(encodeGenePosition(aaMutation), aaMutation);
        }
        List<Integer> foundIds = null;
        try (Connection conn = getDatabaseConnection()) {
            DSLContext ctx = JooqHelper.getDSLCtx(conn);
            YMainAaSequenceColumnar tbl = YMainAaSequenceColumnar.Y_MAIN_AA_SEQUENCE_COLUMNAR;
            Condition condition = DSL.falseCondition();
            for (AAMutation m : aaMutations) {
                condition = condition.or(tbl.GENE.eq(m.getGene()).and(tbl.POSITION.eq(m.getPosition())));
            }
            var statement = ctx
                    .select(tbl.GENE, tbl.POSITION, tbl.DATA_COMPRESSED)
                    .from(tbl)
                    .where(condition);
            for (Record3<String, Integer, byte[]> record : statement.fetch()) {
                String gene = record.value1();
                int position = record.value2();
                byte[] compressed = record.value3();
                char[] aaCodons = aaMutationColumnarCompressor.decompress(compressed).toCharArray();
                AAMutation searchedMutation = genePositionToMutation.get(encodeGenePosition(gene, position));
                if (foundIds == null) {
                    // If this is the first round, search all sequences
                    foundIds = new ArrayList<>();
                    for (int i = 0; i < aaCodons.length; i++) {
                        if (isMatchingMutation(aaCodons[i], searchedMutation)) {
                            foundIds.add(i);
                        }
                    }
                } else {
                    // In the subsequent rounds, we will just continue filter the foundIds.
                    List<Integer> nextFoundIds = new ArrayList<>();
                    for (Integer foundId : foundIds) {
                        if (isMatchingMutation(aaCodons[foundId], searchedMutation)) {
                            nextFoundIds.add(foundId);
                        }
                    }
                    foundIds = nextFoundIds;
                }
                if (foundIds.isEmpty()) {
                    break;
                }
            }
        }
        return foundIds;
    }


    private String encodeGenePosition(AAMutation aaMutation) {
        return encodeGenePosition(aaMutation.getGene(), aaMutation.getPosition());
    }


    private String encodeGenePosition(String gene, int position) {
        return gene + ":" + position;
    }


    private boolean isMatchingMutation(Character foundBase, NucMutation searchedMutation) {
        if (searchedMutation.getMutation() == null) {
            // Check whether the base is mutated, i.e., not equal the base of the reference genome
            throw new RuntimeException("Not implemented");
        } else {
            return foundBase == searchedMutation.getMutation();
        }
    }


    private boolean isMatchingMutation(Character foundBase, AAMutation searchedMutation) {
        if (searchedMutation.getMutation() == null) {
            // Check whether the base is mutated, i.e., not equal the base of the reference genome
            throw new RuntimeException("Not implemented");
        } else {
            return foundBase == searchedMutation.getMutation();
        }
    }


    /**
     * This function translates a pangolin lineage query to an array of SQL like-statements. A sequence matches the
     * query if any like-statements are fulfilled. The like-statements are designed to be passed into the following
     * SQL statement:
     *   where pangolin_lineage like any(?)
     *
     * Prefix search: Return the lineage and all sub-lineages. I.e., for both "B.1.*" and "B.1*", B.1 and
     * all lineages starting with "B.1." should be returned. "B.11" should not be returned.
     *
     * Example: "B.1.2*" will return [B.1.2, B.1.2.%].
     */
    private String[] parsePangolinLineageQuery(String query) {
        String finalQuery = query.toUpperCase();

        // Resolve aliases
        List<String> resolvedQueries = new ArrayList<>() {{
            add(finalQuery);
        }};
        resolvedQueries.addAll(pangolinLineageAliasResolver.findAlias(query));

        // Handle prefix search
        List<String> result = new ArrayList<>();
        for (String resolvedQuery : resolvedQueries) {
            if (resolvedQuery.contains("%")) {
                // Nope, I don't want to allow undocumented features.
            } else if (!resolvedQuery.endsWith("*")) {
                result.add(resolvedQuery);
            } else {
                // Prefix search
                String rootLineage = resolvedQuery.substring(0, resolvedQuery.length() - 1);
                if (rootLineage.endsWith(".")) {
                    rootLineage = rootLineage.substring(0, rootLineage.length() - 1);
                }
                String subLineages = rootLineage + ".%";
                result.add(rootLineage);
                result.add(subLineages);
            }
        }
        return result.toArray(new String[0]);
    }


    public List<PangolinLineageAlias> getPangolinLineageAliases() throws SQLException {
        String sql = """
                select
                  alias,
                  full_name
                from pangolin_lineage_alias;
        """;
        try (Connection conn = getDatabaseConnection()) {
            try (Statement statement = conn.createStatement()) {
                try (ResultSet rs = statement.executeQuery(sql)) {
                    List<PangolinLineageAlias> aliases = new ArrayList<>();
                    while (rs.next()) {
                        aliases.add(new PangolinLineageAlias(
                                rs.getString("alias"),
                                rs.getString("full_name")
                        ));
                    }
                    return aliases;
                }
            }
        }
    }
}
