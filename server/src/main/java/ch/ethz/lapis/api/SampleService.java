package ch.ethz.lapis.api;

import ch.ethz.lapis.LapisMain;
import ch.ethz.lapis.api.entity.AccessKey;
import ch.ethz.lapis.api.entity.DetailsField;
import ch.ethz.lapis.api.entity.OpennessLevel;
import ch.ethz.lapis.api.entity.SequenceType;
import ch.ethz.lapis.api.entity.Versioned;
import ch.ethz.lapis.api.entity.req.BaseSampleRequest;
import ch.ethz.lapis.api.entity.req.OrderAndLimitConfig;
import ch.ethz.lapis.api.entity.req.SampleAggregatedRequest;
import ch.ethz.lapis.api.entity.req.SampleDetailRequest;
import ch.ethz.lapis.api.entity.res.Contributor;
import ch.ethz.lapis.api.entity.res.SampleAggregated;
import ch.ethz.lapis.api.entity.res.SampleDetail;
import ch.ethz.lapis.api.entity.res.SampleMutationsResponse;
import ch.ethz.lapis.api.exception.UnsupportedOrdering;
import ch.ethz.lapis.api.query.Database;
import ch.ethz.lapis.api.query.Database.Columns;
import ch.ethz.lapis.api.query.InsertionStore;
import ch.ethz.lapis.api.query.MutationStore;
import ch.ethz.lapis.api.query.QueryEngine;
import ch.ethz.lapis.util.FastaEntry;
import ch.ethz.lapis.util.ReferenceGenomeData;
import ch.ethz.lapis.util.SeqCompressor;
import ch.ethz.lapis.util.ZstdSeqCompressor;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.Select;
import org.jooq.SelectJoinStep;
import org.jooq.SelectLimitStep;
import org.jooq.SelectOrderByStep;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.jooq.lapis.tables.YMainAaSequence;
import org.jooq.lapis.tables.YMainMetadata;
import org.jooq.lapis.tables.YMainSequence;
import org.jooq.lapis.tables.records.YMainMetadataRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.OutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
public class SampleService {

    private static final ComboPooledDataSource dbPool = LapisMain.dbPool;
    private static final SeqCompressor nucReferenceSeqCompressor
        = new ZstdSeqCompressor(ZstdSeqCompressor.DICT.REFERENCE);
    private static final SeqCompressor AaReferenceSeqCompressor
        = new ZstdSeqCompressor(ZstdSeqCompressor.DICT.AA_REFERENCE);


    private Connection getDatabaseConnection() throws SQLException {
        return dbPool.getConnection();
    }


    public List<AccessKey> getAccessKeys() throws SQLException {
        String sql = "select key, level from access_key;";
        List<AccessKey> keys = new ArrayList<>();
        try (Connection conn = getDatabaseConnection()) {
            try (Statement statement = conn.createStatement()) {
                try (ResultSet rs = statement.executeQuery(sql)) {
                    while (rs.next()) {
                        AccessKey.LEVEL level = switch (rs.getString("level")) {
                            case "full" -> AccessKey.LEVEL.FULL;
                            case "aggregated" -> AccessKey.LEVEL.AGGREGATED;
                            default -> null;
                        };
                        keys.add(new AccessKey(rs.getString("key"), level));
                    }
                }
            }
        }
        return keys;
    }


    public Versioned<List<SampleAggregated>> getAggregatedSamples(SampleAggregatedRequest request) {
        Database database = Database.getOrLoadInstance(dbPool);
        return new Versioned<>(database.getDataVersion(), new QueryEngine().aggregate(database, request));
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

        List<SampleDetail> samples = new ArrayList<>();
        try (Connection conn = getDatabaseConnection()) {
            DSLContext ctx = JooqHelper.getDSLCtx(conn);
            YMainMetadata tbl = YMainMetadata.Y_MAIN_METADATA;

            Collection<DetailsField<?>> selectedDetailsFields = DetailsField.getDetails(request.getFields());

            List<Field<?>> selectFields = selectedDetailsFields.stream()
                .map(field -> field.databaseColumnName)
                .collect(toList());

            Table<Record1<Integer>> idsTbl = getIdsTable(ids, ctx);
            SelectJoinStep<Record> statement = ctx
                .select(selectFields)
                .from(idsTbl.join(tbl).on(idsTbl.field("id", Integer.class).eq(tbl.ID)));
            Select<Record> orderedAndLimitedStatement = applyOrderAndLimit(statement, orderAndLimit);
            Result<Record> records = orderedAndLimitedStatement.fetch();
            for (var record : records) {
                var sample = new SampleDetail();
                selectedDetailsFields.forEach(detailsField -> detailsField.setDataFromRecord(sample, record));

                samples.add(sample);
            }
        }
        return samples;
    }


    public List<Contributor> getContributors(
        BaseSampleRequest request,
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
        BaseSampleRequest request,
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
        return ids.stream().map(id -> strainColumn[id]).collect(toList());
    }


    public List<String> getGisaidEpiIsls(
        BaseSampleRequest request,
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
        return ids.stream().map(id -> gisaidEpiIslColumn[id]).collect(toList());
    }


    public List<String> getGenbankAccessions(
        BaseSampleRequest request,
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
        String[] genbankAccessionColumn = database.getStringColumn(Columns.GENBANK_ACCESSION);
        return ids.stream().map(id -> genbankAccessionColumn[id]).collect(toList());
    }


    public SampleMutationsResponse getMutations(
        BaseSampleRequest request,
        SequenceType sequenceType,
        float minProportion
    ) {
        Database database = Database.getOrLoadInstance(dbPool);
        ReferenceGenomeData reference = ReferenceGenomeData.getInstance();
        // Filter
        List<Integer> ids = new QueryEngine().filterIds(database, request);
        if (ids.isEmpty()) {
            return new SampleMutationsResponse();
        }
        // Count mutations
        SampleMutationsResponse response = new SampleMutationsResponse();
        if (sequenceType == SequenceType.NUCLEOTIDE) {
            List<MutationStore.MutationCount> mutationCounts = database.getNucMutationStore().countMutations(ids);
            for (MutationStore.MutationCount mutationCount : mutationCounts) {
                if (mutationCount.getProportion() < minProportion) {
                    continue;
                }
                MutationStore.Mutation mutation = mutationCount.getMutation();
                String mutString = "%s%s%s".formatted(
                    reference.getNucleotideBase(mutation.position),
                    mutation.position,
                    mutation.mutationTo
                );
                response.add(new SampleMutationsResponse.MutationEntry(mutString,
                    mutationCount.getProportion(), mutationCount.getCount()));
            }
        } else {
            database.getAaMutationStores().forEach((gene, mutationStore) -> {
                List<MutationStore.MutationCount> mutationCounts = mutationStore.countMutations(ids);
                for (MutationStore.MutationCount mutationCount : mutationCounts) {
                    if (mutationCount.getProportion() < minProportion) {
                        continue;
                    }
                    MutationStore.Mutation mutation = mutationCount.getMutation();
                    String mutString = "%s:%s%s%s".formatted(
                        gene,
                        reference.getGeneAABase(gene, mutation.position),
                        mutation.position,
                        mutation.mutationTo
                    );
                    response.add(new SampleMutationsResponse.MutationEntry(mutString,
                        mutationCount.getProportion(), mutationCount.getCount()));
                }
            });
        }
        return response;
    }


    public List<InsertionStore.InsertionCount> getInsertions(
        BaseSampleRequest request,
        SequenceType sequenceType
    ) {
        Database database = Database.getOrLoadInstance(dbPool);
        List<Integer> ids = new QueryEngine().filterIds(database, request);
        if (ids.isEmpty()) {
            return List.of();
        }
        if (sequenceType == SequenceType.NUCLEOTIDE) {
            return database.getNucInsertionStore().countInsertions(ids).stream()
                // Append "ins_" to the insertion string
                .map(ins -> new InsertionStore.InsertionCount(
                    "ins_" + ins.insertion(),
                    ins.count()
                ))
                .collect(toList());
        } else {
            List<InsertionStore.InsertionCount> result = new ArrayList<>();
            database.getAaInsertionStores().forEach((gene, store) -> {
                var insertionsOfGene = store.countInsertions(ids).stream()
                    // Append "ins_" and the gene to the insertion string
                    .map(ins -> new InsertionStore.InsertionCount(
                        "ins_" + gene + ":" + ins.insertion(),
                        ins.count()
                    ))
                    .toList();
                result.addAll(insertionsOfGene);
            });
            return result;
        }
    }

    public void getNucSequencesInFastaFormat(
        BaseSampleRequest request,
        boolean aligned,
        OrderAndLimitConfig orderAndLimit,
        OutputStream outputStream
    ) {
        var filteredIds = new QueryEngine().filterIds(Database.getOrLoadInstance(dbPool), request);
        if (filteredIds.isEmpty()) {
            return;
        }

        runInDatabaseContextWithoutAutoCommit(ctx -> {
            var metaDataTable = YMainMetadata.Y_MAIN_METADATA;
            var sequenceTable = YMainSequence.Y_MAIN_SEQUENCE;

            var sequenceColumn = aligned ? sequenceTable.SEQ_ALIGNED_COMPRESSED : sequenceTable.SEQ_ORIGINAL_COMPRESSED;

            var idsTable = getIdsTable(filteredIds, ctx);
            var sequenceIdentifierColumn = LapisMain.globalConfig.getApiOpennessLevel() == OpennessLevel.OPEN
                ? metaDataTable.STRAIN
                : metaDataTable.GISAID_EPI_ISL;

            var statement = ctx
                .select(sequenceIdentifierColumn, sequenceColumn)
                .from(
                    idsTable
                        .join(metaDataTable).on(idsTable.field("id", Integer.class).eq(metaDataTable.ID))
                        .join(sequenceTable).on(metaDataTable.ID.eq(sequenceTable.ID))
                );

            streamFastaSequences(
                orderAndLimit,
                outputStream,
                sequenceColumn,
                sequenceIdentifierColumn,
                statement,
                nucReferenceSeqCompressor
            );
        });
    }

    public StreamingResponseBody getAaSequencesInFastaFormatStream(
        BaseSampleRequest request,
        OrderAndLimitConfig orderAndLimit,
        String gene
    ) {
        var filteredIds = new QueryEngine().filterIds(Database.getOrLoadInstance(dbPool), request);
        if (filteredIds.isEmpty()) {
            return outputStream -> {
            };
        }
        return outputStream -> runInDatabaseContextWithoutAutoCommit(ctx -> {
            var metaDataTable = YMainMetadata.Y_MAIN_METADATA;
            var aaSequenceTable = YMainAaSequence.Y_MAIN_AA_SEQUENCE;

            var sequenceIdentifierColumn = LapisMain.globalConfig.getApiOpennessLevel() == OpennessLevel.OPEN
                ? metaDataTable.STRAIN
                : metaDataTable.GISAID_EPI_ISL;
            var aaSequenceColumn = aaSequenceTable.AA_SEQ_COMPRESSED;

            var joinCondition = aaSequenceTable.GENE.eq(gene)
                .and(aaSequenceTable.ID.in(filteredIds))
                .and(metaDataTable.ID.eq(aaSequenceTable.ID));

            var statement = ctx.select(sequenceIdentifierColumn, aaSequenceColumn)
                .from(aaSequenceTable.join(metaDataTable).on(joinCondition));

            streamFastaSequences(
                orderAndLimit,
                outputStream,
                aaSequenceColumn,
                sequenceIdentifierColumn,
                statement,
                AaReferenceSeqCompressor
            );
        });
    }

    private void runInDatabaseContextWithoutAutoCommit(Consumer<DSLContext> databaseQuery) {
        Connection conn = null;
        try {
            conn = getDatabaseConnection();
            conn.setAutoCommit(false);
            DSLContext ctx = JooqHelper.getDSLCtx(conn);
            databaseQuery.accept(ctx);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    private <SequenceRecord extends Record> void streamFastaSequences(
        OrderAndLimitConfig orderAndLimit,
        OutputStream outputStream,
        TableField<SequenceRecord, byte[]> sequenceColumn,
        TableField<YMainMetadataRecord, String> sequenceIdentifierColumn,
        SelectJoinStep<Record2<String, byte[]>> statement,
        SeqCompressor compressor
    ) {
        if (orderAndLimit.getLimit() == null) {
            orderAndLimit.setLimit(100000);
        }
        var orderedAndLimitedstatement = applyOrderAndLimit(statement, orderAndLimit);

        try (var cursor = orderedAndLimitedstatement.fetchSize(1000).fetchLazy()) {
            for (var row : cursor) {
                var fastaEntry = new FastaEntry(
                    row.get(sequenceIdentifierColumn),
                    compressor.decompress(row.get(sequenceColumn))
                );
                fastaEntry.writeToStream(outputStream);
            }
        }
    }

    public String getNextcladeDatasetTag() {
        String loadOldNextcladeDatasetTagSql = """
                select tag
                from nextclade_dataset_version
                order by inserted_at desc
                limit 1;
            """;
        String tag = null;
        try (Connection conn = getDatabaseConnection()) {
            try (Statement statement = conn.createStatement()) {
                try (ResultSet rs = statement.executeQuery(loadOldNextcladeDatasetTagSql)) {
                    if (rs.next()) {
                        tag = rs.getString("tag");
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return tag;
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
            copy = copy.subList(0, Math.min(orderAndLimitConfig.getLimit(), copy.size()));
        }
        return copy;
    }
}
