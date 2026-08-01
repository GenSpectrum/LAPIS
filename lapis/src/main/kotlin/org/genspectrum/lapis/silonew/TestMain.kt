package org.genspectrum.lapis.silonew

import kotlinx.datetime.Month
import kotlinx.datetime.toJavaLocalDate

// JVM args: -Dio.netty.tryReflectionSetAccessible=true --add-opens=java.base/java.nio=ALL-UNNAMED
fun main() {
    // Database and schema
    val db = Database("https://gs-staging-1.int.genspectrum.org/open/v2/silo")
    val schema = db.getSchema()
    println(">> Schema: $schema")

    // Helper variables
    val defaultTbl = schema.table("default")
    val countryCol = defaultTbl["country"]
    val dateCol = defaultTbl["date"]
    val strainCol = defaultTbl["strain"]
    val spikeSequenceCol = defaultTbl["S"]
    val countCol = field("count")

    // Queries
    val advancedFilter = parseAdvancedQuery(
        advancedQuery = "region=Europe and not country=Germany and dateSubmittedYear>=2026",
        table = defaultTbl,
    )
    val query1 = defaultTbl
        .filter(
            advancedFilter and
                (dateCol gte literal(kotlinx.datetime.LocalDate(2026, Month.JANUARY, 1).toJavaLocalDate())),
        )
        .groupBy(
            aggregates = record(countCol assign count()),
            columns = set(countryCol),
        )
        .orderBy(desc(countCol))
    val query2 = defaultTbl
        .project(strainCol, countryCol, dateCol, spikeSequenceCol)
        .limit(3)

    println(">> Query: ${query1.render()}")
    // SiloRows
    db.sendQuery(query1).use { result ->
        println(">> Data version: ${result.dataVersion}")
        result.rows.forEach { row ->
            println("(${row.getString("country")},${row.getLong("count")})")
        }
    }
    // TSV
    db.sendQuery(query1, SiloDataFormat.Tsv()).use { result ->
        println(">> Data version: ${result.dataVersion}")
        result.writeTo(System.out)
    }

    println(">> Query: ${query2.render()}")
    // FASTA
    db.sendQuery(
        query = query2,
        format = SiloDataFormat.Fasta(
            sequenceFields = listOf(spikeSequenceCol),
            headerTemplate = "{strain}|{country}|{date}",
        ),
    ).use { result ->
        println(">> Data version: ${result.dataVersion}")
        result.writeTo(System.out)
    }
}
