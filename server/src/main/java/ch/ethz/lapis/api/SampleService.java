package ch.ethz.lapis.api;

import ch.ethz.lapis.LapisMain;
import ch.ethz.lapis.api.entity.SequenceType;
import ch.ethz.lapis.api.entity.req.OrderAndLimitConfig;
import ch.ethz.lapis.api.entity.req.SampleAggregatedRequest;
import ch.ethz.lapis.api.entity.req.SampleDetailRequest;
import ch.ethz.lapis.api.entity.res.Contributor;
import ch.ethz.lapis.api.entity.res.SampleAggregated;
import ch.ethz.lapis.api.entity.res.SampleDetail;
import ch.ethz.lapis.api.entity.res.SampleMutationsResponse;
import ch.ethz.lapis.api.exception.UnsupportedOrdering;
import ch.ethz.lapis.api.query.Database;
import ch.ethz.lapis.api.query.QueryEngine;
import ch.ethz.lapis.util.SeqCompressor;
import ch.ethz.lapis.util.ZstdSeqCompressor;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.jooq.Record;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.lapis.tables.YMainMetadata;
import org.jooq.lapis.tables.YMainSequence;
import org.jooq.lapis.tables.records.YMainSequenceRecord;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SampleService {

    private static final ComboPooledDataSource dbPool = LapisMain.dbPool;
    private static final SeqCompressor referenceSeqCompressor
        = new ZstdSeqCompressor(ZstdSeqCompressor.DICT.REFERENCE);


    private Connection getDatabaseConnection() throws SQLException {
        return dbPool.getConnection();
    }


    public List<SampleAggregated> getAggregatedSamples(
        SampleAggregatedRequest request
    ) throws SQLException {
        return new QueryEngine().aggregate(Database.getOrLoadInstance(dbPool), request);
    }


    public List<SampleDetail> getDetailedSamples(
        SampleDetailRequest request,
        OrderAndLimitConfig orderAndLimit
    ) throws SQLException {
        // Filter
        List<Integer> ids = new QueryEngine().filterIds(Database.getOrLoadInstance(dbPool), request);
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }

        // Fetch data
        List<SampleDetail> samples = new ArrayList<>();
        try (Connection conn = getDatabaseConnection()) {
            DSLContext ctx = JooqHelper.getDSLCtx(conn);
            YMainMetadata tbl = YMainMetadata.Y_MAIN_METADATA;

            List<Field<?>> selectFields = new ArrayList<>() {{
                add(tbl.GENBANK_ACCESSION);
                add(tbl.SRA_ACCESSION);
                add(tbl.GISAID_EPI_ISL);
                add(tbl.STRAIN);
                add(tbl.DATE);
                add(tbl.DATE_SUBMITTED);
                add(tbl.REGION);
                add(tbl.COUNTRY);
                add(tbl.DIVISION);
                add(tbl.LOCATION);
                add(tbl.REGION_EXPOSURE);
                add(tbl.COUNTRY_EXPOSURE);
                add(tbl.DIVISION_EXPOSURE);
                add(tbl.AGE);
                add(tbl.SEX);
                add(tbl.HOSPITALIZED);
                add(tbl.DIED);
                add(tbl.FULLY_VACCINATED);
                add(tbl.HOST);
                add(tbl.SAMPLING_STRATEGY);
                add(tbl.PANGO_LINEAGE);
                add(tbl.NEXTSTRAIN_CLADE);
                add(tbl.GISAID_CLADE);
                add(tbl.SUBMITTING_LAB);
                add(tbl.ORIGINATING_LAB);
            }};

            Table<Record1<Integer>> idsTbl = getIdsTable(ids, ctx);
            SelectJoinStep<Record> statement = ctx
                .select(selectFields)
                .from(idsTbl.join(tbl).on(idsTbl.field("id", Integer.class).eq(tbl.ID)));
            Select<Record> statement2 = applyOrderAndLimit(statement, orderAndLimit);
            Result<Record> records = statement2.fetch();
            for (var r : records) {
                SampleDetail sample = new SampleDetail()
                    .setGenbankAccession(r.get(tbl.GENBANK_ACCESSION))
                    .setSraAccession(r.get(tbl.SRA_ACCESSION))
                    .setGisaidEpiIsl(r.get(tbl.GISAID_EPI_ISL))
                    .setStrain(r.get(tbl.STRAIN))
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
                    .setHospitalized(r.get(tbl.HOSPITALIZED))
                    .setDied(r.get(tbl.DIED))
                    .setFullyVaccinated(r.get(tbl.FULLY_VACCINATED))
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


    public List<Contributor> getContributors(
        SampleDetailRequest request,
        OrderAndLimitConfig orderAndLimit
    ) throws SQLException {
        // Filter
        List<Integer> ids = new QueryEngine().filterIds(Database.getOrLoadInstance(dbPool), request);
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }

        // Fetch data
        List<Contributor> contributors = new ArrayList<>();
        try (Connection conn = getDatabaseConnection()) {
            DSLContext ctx = JooqHelper.getDSLCtx(conn);
            YMainMetadata tbl = YMainMetadata.Y_MAIN_METADATA;

            List<Field<?>> selectFields = new ArrayList<>() {{
                add(tbl.GENBANK_ACCESSION);
                add(tbl.SRA_ACCESSION);
                add(tbl.GISAID_EPI_ISL);
                add(tbl.STRAIN);
                add(tbl.SUBMITTING_LAB);
                add(tbl.ORIGINATING_LAB);
                add(tbl.AUTHORS);
            }};

            Table<Record1<Integer>> idsTbl = getIdsTable(ids, ctx);
            SelectJoinStep<Record> statement = ctx
                .select(selectFields)
                .from(idsTbl.join(tbl).on(idsTbl.field("id", Integer.class).eq(tbl.ID)));
            Select<Record> statement2 = applyOrderAndLimit(statement, orderAndLimit);
            Result<Record> records = statement2.fetch();
            for (var r : records) {
                Contributor contributor = new Contributor()
                    .setGenbankAccession(r.get(tbl.GENBANK_ACCESSION))
                    .setSraAccession(r.get(tbl.SRA_ACCESSION))
                    .setGisaidEpiIsl(r.get(tbl.GISAID_EPI_ISL))
                    .setStrain(r.get(tbl.STRAIN))
                    .setSubmittingLab(r.get(tbl.SUBMITTING_LAB))
                    .setOriginatingLab(r.get(tbl.ORIGINATING_LAB))
                    .setAuthors(r.get(tbl.AUTHORS));
                contributors.add(contributor);
            }
        }
        return contributors;
    }


    public List<String> getStrainNames(
        SampleDetailRequest request,
        OrderAndLimitConfig orderAndLimit
    ) {
        Database database = Database.getOrLoadInstance(dbPool);
        // Filter
        List<Integer> ids = new QueryEngine().filterIds(database, request);
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        // Order and limit
        ids = applyOrderAndLimit(ids, orderAndLimit);
        // Fetch data
        String[] strainColumn = database.getStringColumn(Database.Columns.STRAIN);
        return ids.stream().map(id -> strainColumn[id]).collect(Collectors.toList());
    }


    public List<String> getGisaidEpiIsls(
        SampleDetailRequest request,
        OrderAndLimitConfig orderAndLimit
    ) {
        Database database = Database.getOrLoadInstance(dbPool);
        // Filter
        List<Integer> ids = new QueryEngine().filterIds(database, request);
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        // Order and limit
        ids = applyOrderAndLimit(ids, orderAndLimit);
        // Fetch data
        String[] gisaidEpiIslColumn = database.getStringColumn(Database.Columns.GISAID_EPI_ISL);
        return ids.stream().map(id -> gisaidEpiIslColumn[id]).collect(Collectors.toList());
    }


    public SampleMutationsResponse getMutations(
        SampleDetailRequest request,
        SequenceType sequenceType,
        float minProportion
    ) throws SQLException {
        // Filter
        List<Integer> ids = new QueryEngine().filterIds(Database.getOrLoadInstance(dbPool), request);
        if (ids.isEmpty()) {
            return new SampleMutationsResponse();
        }

        // Fetch data
        try (Connection conn = getDatabaseConnection()) {
            DSLContext ctx = JooqHelper.getDSLCtx(conn);
            YMainMetadata metaTbl = YMainMetadata.Y_MAIN_METADATA;
            YMainSequence seqTbl = YMainSequence.Y_MAIN_SEQUENCE;

            TableOnConditionStep<Record> baseTbl;
            Table<Record1<Integer>> idsTbl = getIdsTable(ids, ctx);
            baseTbl = idsTbl
                .join(metaTbl).on(idsTbl.field("id", Integer.class).eq(metaTbl.ID))
                .join(seqTbl).on(metaTbl.ID.eq(seqTbl.ID));
            String mutationColumnName = switch (sequenceType) {
                case AMINO_ACID -> "aa_mutations";
                case NUCLEOTIDE -> "nuc_substitutions || ',' || nuc_deletions";
            };
            var statement = ctx
                        .select(DSL.field(mutationColumnName).cast(String.class))
                        .from(baseTbl);
            Result<Record1<String>> records = statement.fetch();
            int count = 0;
            Map<String, int[]> mutations = new HashMap<>();
            for (var r : records) {
                for (String mut : r.value1().split(",")) {
                    if (mut.isBlank()) {
                        continue;
                    }
                    mutations.compute(mut, (k, v) -> v == null ?
                        new int[] { 0 } : v)[0]++;
                }
                count++;
            }
            int minCount = (int) Math.ceil(minProportion * count);
            List<SampleMutationsResponse.MutationEntry> mutationEntries = new ArrayList<>();
            for (Map.Entry<String, int[]> entry : mutations.entrySet()) {
                int mutCount = entry.getValue()[0];
                if (mutCount < minCount) {
                    continue;
                }
                String mut = entry.getKey();
                mutationEntries.add(new SampleMutationsResponse.MutationEntry(mut, mutCount * 1.0 / count, mutCount));
            }
            return new SampleMutationsResponse(mutationEntries);
        }
    }


    public void getFasta(
        SampleDetailRequest request,
        boolean aligned,
        OrderAndLimitConfig orderAndLimit,
        OutputStream outputStream
    ) {
        // Filter
        List<Integer> ids = new QueryEngine().filterIds(Database.getOrLoadInstance(dbPool), request);
        if (ids.isEmpty()) {
            return;
        }

        // Filter further by the other metadata and prepare the response
        Connection conn = null;
        try {
            conn = getDatabaseConnection();
            conn.setAutoCommit(false);
            DSLContext ctx = JooqHelper.getDSLCtx(conn);
            YMainMetadata metaTbl = YMainMetadata.Y_MAIN_METADATA;
            YMainSequence seqTbl = YMainSequence.Y_MAIN_SEQUENCE;

            TableField<YMainSequenceRecord, byte[]> seqColumn = aligned ?
                seqTbl.SEQ_ALIGNED_COMPRESSED : seqTbl.SEQ_ORIGINAL_COMPRESSED;

            SelectLimitPercentStep<Record2<String, byte[]>> statement;
            Table<Record1<Integer>> idsTbl = getIdsTable(ids, ctx);
            statement = ctx
                .select(metaTbl.GENBANK_ACCESSION, seqColumn)
                .from(
                    idsTbl
                        .join(metaTbl).on(idsTbl.field("id", Integer.class).eq(metaTbl.ID))
                        .join(seqTbl).on(metaTbl.ID.eq(seqTbl.ID))
                )
                .limit(orderAndLimit.getLimit() != null ? Math.min(100000, orderAndLimit.getLimit()) : 100000);
            Cursor<Record2<String, byte[]>> cursor = statement.fetchSize(1000).fetchLazy();
            for (Record2<String, byte[]> r : cursor) {
                outputStream.write(">".getBytes(StandardCharsets.UTF_8));
                outputStream.write(r.get(metaTbl.GENBANK_ACCESSION).getBytes(StandardCharsets.UTF_8));
                outputStream.write("\n".getBytes(StandardCharsets.UTF_8));
                outputStream.write(referenceSeqCompressor.decompress(r.get(seqColumn))
                    .getBytes(StandardCharsets.UTF_8));
                outputStream.write("\n\n".getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException | SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }


    private Table<Record1<Integer>> getIdsTable(Collection<Integer> ids, DSLContext ctx) {
        String idsStr = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        return ctx
            .select(DSL.field("i.id::integer", Integer.class).as("id"))
            // We are concatenating SQL here!
            // This is safe because the IDs are read from the database and were generated and then written
            // by this program into the database. Further, the IDs are guaranteed to be integers.
            .from("unnest(string_to_array('" + idsStr + "', ',')) i(id)")
            .asTable("ids");
    }


    private <T extends Record> Select<T> applyOrderAndLimit(
        SelectOrderByStep<T> statement,
        OrderAndLimitConfig orderAndLimitConfig
    ) {
        // orderBy
        SelectLimitStep<T> statement2 = statement;
        String orderBy = orderAndLimitConfig.getOrderBy();
        if (orderBy != null && !orderBy.isBlank() && !orderBy.equals(OrderAndLimitConfig.SpecialOrdering.ARBITRARY)) {
            if (orderBy.equals(OrderAndLimitConfig.SpecialOrdering.RANDOM)) {
                SelectSeekStep1<T, BigDecimal> x;
                statement2 = statement.orderBy(DSL.rand());
            } else {
                throw new UnsupportedOrdering(orderBy);
            }
        }

        // limit
        Select<T> statement3 = statement2;
        if (orderAndLimitConfig.getLimit() != null) {
            statement3 = statement2.limit(orderAndLimitConfig.getLimit());
        }

        return statement3;
    }


    private <T> List<T> applyOrderAndLimit(List<T> data, OrderAndLimitConfig orderAndLimitConfig) {
        List<T> copy = new ArrayList<>(data);
        // orderBy
        String orderBy = orderAndLimitConfig.getOrderBy();
        if (orderBy != null && !orderBy.isBlank() && !orderBy.equals(OrderAndLimitConfig.SpecialOrdering.ARBITRARY)) {
            if (orderBy.equals(OrderAndLimitConfig.SpecialOrdering.RANDOM)) {
                Collections.shuffle(copy);
            } else {
                throw new UnsupportedOrdering(orderBy);
            }
        }
        // limit
        if (orderAndLimitConfig.getLimit() != null) {
            copy = copy.subList(0, orderAndLimitConfig.getLimit());
        }
        return copy;
    }
}
