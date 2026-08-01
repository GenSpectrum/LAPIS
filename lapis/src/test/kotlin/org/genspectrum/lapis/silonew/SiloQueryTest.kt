package org.genspectrum.lapis.silonew

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SiloQueryTest {
    private val schema = siloSchema {
        table("sequences") {
            column("primary_key", SiloColumnType.STRING)
            column("country", SiloColumnType.INDEXED_STRING)
            column("date", SiloColumnType.DATE32)
            column("pango_lineage", SiloColumnType.INDEXED_STRING)
            column("usherTree", SiloColumnType.STRING)
            column("main", SiloColumnType.STRING)
        }
        table("samples") {
            column("sample_id", SiloColumnType.STRING)
            column("country", SiloColumnType.INDEXED_STRING)
        }
    }
    private val sequences = schema.table("sequences")
    private val samples = schema.table("samples")
    private val primaryKey = sequences["primary_key"]
    private val country = sequences["country"]
    private val date = sequences["date"]
    private val lineage = sequences["pango_lineage"]
    private val usherTree = sequences["usherTree"]
    private val mainSequence = sequences["main"]
    private val sampleId = samples["sample_id"]

    @Test
    fun `GIVEN a query THEN renders a complete aggregation pipeline`() {
        val count = field("count")
        val query = sequences
            .filter(country eq literal("Switzerland"))
            .groupBy(
                aggregates = record(count assign count()),
                columns = set(country),
            )
            .orderBy(desc(count))
            .offset(10)
            .limit(5)

        assertThat(
            query.render(),
            equalTo(
                "sequences.filter(\"country\" = 'Switzerland')" +
                    ".groupBy({\"count\":=count()}, {\"country\"})" +
                    ".orderBy({desc(\"count\")}).offset(10).limit(5)",
            ),
        )
    }

    @Test
    fun `GIVEN relational transformations THEN renders projection map ordering and randomization`() {
        val week = field("week")
        val query = sequences
            .map(week assign date.isoWeek())
            .project(primaryKey, country, week)
            .orderBy(asc(country), desc(week))
            .randomize(seed = 42)

        assertThat(
            query.render(),
            equalTo(
                "sequences.map({\"week\":=\"date\".isoWeek()})" +
                    ".project({\"primary_key\", \"country\", \"week\"})" +
                    ".orderBy({asc(\"country\"), desc(\"week\")}).randomize(seed:=42)",
            ),
        )
    }

    @Test
    fun `GIVEN unions and schema operation THEN composes multiple tables and nested relations`() {
        val query = unionAll(
            left = sequences.filter(country eq literal("CH")).project(country),
            right = samples.project(samples["country"]),
        ).schema()

        assertThat(
            query.render(),
            equalTo(
                "unionAll(sequences.filter(\"country\" = 'CH').project({\"country\"}), " +
                    "samples.project({\"country\"})).schema()",
            ),
        )
    }

    @Test
    fun `GIVEN two relations THEN renders standalone and piped joins`() {
        val leftId = field("left_id")
        val rightId = field("right_id")
        val left = sequences.project(leftId)
        val right = samples.project(rightId)
        val condition = (leftId eq rightId) and (field("country\"name") neq literal("unknown"))

        assertThat(
            join(left, right, condition).render(),
            equalTo(
                "join(sequences.project({\"left_id\"}), samples.project({\"right_id\"}), " +
                    "\"left_id\" = \"right_id\" && \"country\"\"name\" <> 'unknown', type:=inner)",
            ),
        )
        assertThat(
            left.join(right, condition, JoinType.LEFT_ANTI).render(),
            equalTo(
                "sequences.project({\"left_id\"}).join(samples.project({\"right_id\"}), " +
                    "\"left_id\" = \"right_id\" && \"country\"\"name\" <> 'unknown', type:=leftAnti)",
            ),
        )
    }

    @Test
    fun `GIVEN join types THEN renders every SILO join token`() {
        val expectedTokens = mapOf(
            JoinType.INNER to "inner",
            JoinType.LEFT to "left",
            JoinType.RIGHT to "right",
            JoinType.FULL to "full",
            JoinType.LEFT_SEMI to "leftSemi",
            JoinType.RIGHT_SEMI to "rightSemi",
            JoinType.LEFT_ANTI to "leftAnti",
            JoinType.RIGHT_ANTI to "rightAnti",
        )

        expectedTokens.forEach { (type, token) ->
            assertThat(
                sequences.join(samples, primaryKey eq sampleId, type).render(),
                equalTo("sequences.join(samples, \"primary_key\" = \"sample_id\", type:=$token)"),
            )
        }
    }

    @Test
    fun `GIVEN specialized result operations THEN renders all documented relation operators`() {
        assertThat(
            sequences.mutations(
                minProportion = 0.05,
                sequenceNames = listOf(mainSequence),
                fields = listOf(field("mutation"), field("count")),
            ).render(),
            equalTo(
                "sequences.mutations(minProportion:=0.05, sequenceNames:={\"main\"}, " +
                    "fields:={\"mutation\", \"count\"})",
            ),
        )
        assertThat(sequences.aminoAcidMutations().render(), equalTo("sequences.aminoAcidMutations()"))
        assertThat(
            sequences.insertions(listOf(mainSequence)).render(),
            equalTo("sequences.insertions(sequenceNames:={\"main\"})"),
        )
        assertThat(sequences.aminoAcidInsertions().render(), equalTo("sequences.aminoAcidInsertions()"))
        assertThat(
            sequences.mostRecentCommonAncestor(usherTree, printNodesNotInTree = true).render(),
            equalTo("sequences.mostRecentCommonAncestor('usherTree', printNodesNotInTree:=true)"),
        )
        assertThat(
            sequences.phyloSubtree(
                column = usherTree,
                printNodesNotInTree = false,
                contractUnaryNodes = true,
            ).render(),
            equalTo(
                "sequences.phyloSubtree('usherTree', printNodesNotInTree:=false, contractUnaryNodes:=true)",
            ),
        )
    }

    @Test
    fun `GIVEN scalar helpers THEN renders documented metadata predicates and mappings`() {
        val predicate =
            date.between(literal(LocalDate.of(2021, 1, 1)), nullLiteral()) and
                country.isIn(literal("CH"), literal("DE")) and
                country.isNotNull() and
                country.like("Basel.*") and
                lineage.lineage(
                    value = "XBB",
                    includeSublineages = true,
                    recombinantFollowingMode = "alwaysFollow",
                ) and
                usherTree.phyloDescendantOf("NODE_1")

        assertThat(
            predicate.render(),
            equalTo(
                "\"date\".between('2021-01-01'::date, null) && \"country\".in({'CH', 'DE'}) && " +
                    "isNotNull(\"country\") && \"country\".like('Basel.*') && " +
                    "\"pango_lineage\".lineage('XBB', includeSublineages:=true, " +
                    "recombinantFollowingMode:='alwaysFollow') && \"usherTree\".phyloDescendantOf('NODE_1')",
            ),
        )
        assertThat(primaryKey.at(2).render(), equalTo("\"primary_key\".at(2)"))
        assertThat(date.isNull().render(), equalTo("isNull(\"date\")"))
    }

    @Test
    fun `GIVEN genomic predicates THEN renders all documented sequence predicate forms`() {
        val query = sequences.filter(
            maybe(nucleotideEquals(position = 300, symbol = "G", sequenceName = "main")) and
                exact(aminoAcidEquals(position = 501, symbol = "Y", sequenceName = "S")) and
                hasMutation(position = 23403) and
                hasAminoAcidMutation(position = 501, sequenceName = "S") and
                insertionContains(position = 22204, value = "AGT") and
                aminoAcidInsertionContains(position = 214, value = ".*EPE", sequenceName = "S"),
        )

        assertThat(
            query.render(),
            equalTo(
                "sequences.filter(maybe(nucleotideEquals(position:=300, symbol:='G', sequenceName:='main')) && " +
                    "exact(aminoAcidEquals(position:=501, symbol:='Y', sequenceName:='S')) && " +
                    "hasMutation(position:=23403) && hasAAMutation(position:=501, sequenceName:='S') && " +
                    "insertionContains(position:=22204, value:='AGT') && " +
                    "aminoAcidInsertionContains(position:=214, value:='.*EPE', sequenceName:='S'))",
            ),
        )
    }

    @Test
    fun `GIVEN compound and profile predicates THEN renders records and optional profile inputs`() {
        val mutations = listOf(mutation(position = 241, symbol = "T"), mutation(position = 23403, symbol = "G"))

        assertThat(
            nOf(2, listOf(hasMutation(241), hasMutation(3037)), matchExactly = true).render(),
            equalTo("nOf(2, {hasMutation(position:=241), hasMutation(position:=3037)}, matchExactly:=true)"),
        )
        assertThat(
            nucleotideMutationProfile(distance = 3, sequenceName = "main", mutations = mutations).render(),
            equalTo(
                "nucleotideMutationProfile(distance:=3, sequenceName:='main', " +
                    "mutations:={{\"position\":=241, \"symbol\":='T'}, " +
                    "{\"position\":=23403, \"symbol\":='G'}})",
            ),
        )
        assertThat(
            aminoAcidMutationProfile(distance = 2, querySequence = "MYSEQ").render(),
            equalTo("aminoAcidMutationProfile(distance:=2, querySequence:='MYSEQ')"),
        )
        assertThat(
            nucleotideMutationProfile(distance = 1, sequenceId = "key_1").render(),
            equalTo("nucleotideMutationProfile(distance:=1, sequenceId:='key_1')"),
        )
    }

    @Test
    fun `GIVEN generic operations THEN keeps future calls structured and escaped`() {
        val query = relationCall(
            name = "future source",
            namedArguments = listOf(namedArgument("table);evil", literal("value"))),
        ).pipe(
            name = "future operator",
            positionalArguments = listOf(field("column);evil")),
        )

        assertThat(
            query.render(),
            equalTo("\"future source\"(\"table);evil\":='value').\"future operator\"(\"column);evil\")"),
        )
    }

    @Test
    fun `GIVEN direct table and field references THEN escapes both identifiers`() {
        val hostileTable = siloSchema {
            table("default.filter(true)") {
                column("country\"name", SiloColumnType.STRING)
            }
        }.table("default.filter(true)")
        val query = hostileTable.filter(hostileTable["country\"name"] eq literal("CH"))

        assertThat(
            query.render(),
            equalTo("\"default.filter(true)\".filter(\"country\"\"name\" = 'CH')"),
        )
    }

    @Test
    fun `GIVEN a reused relation THEN appending operations does not mutate the original`() {
        val filtered = sequences.filter(country eq literal("CH"))
        val first = filtered.limit(1)
        val second = filtered.limit(2)

        assertThat(filtered.render(), equalTo("sequences.filter(\"country\" = 'CH')"))
        assertThat(first.render(), equalTo("sequences.filter(\"country\" = 'CH').limit(1)"))
        assertThat(second.render(), equalTo("sequences.filter(\"country\" = 'CH').limit(2)"))
    }
}
