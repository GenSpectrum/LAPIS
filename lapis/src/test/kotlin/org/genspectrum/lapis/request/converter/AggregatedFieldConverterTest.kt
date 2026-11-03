package org.genspectrum.lapis.request.converter

import org.genspectrum.lapis.config.DatabaseMetadata
import org.genspectrum.lapis.config.MetadataType
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.config.ReferenceSequenceSchema
import org.genspectrum.lapis.databaseConfig
import org.genspectrum.lapis.request.LineageWithSublineagesField
import org.genspectrum.lapis.request.PlainField
import org.genspectrum.lapis.request.SequencePositionField
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class AggregatedFieldConverterTest {
    private val dbConfig = databaseConfig(
        primaryKey = "primaryKey",
        metadata = listOf(
            DatabaseMetadata(name = "primaryKey", type = MetadataType.STRING),
            DatabaseMetadata(
                name = "pangoLineage",
                type = MetadataType.STRING,
                generateLineageIndex = "lineage_definitions",
            ),
        ),
    )

    private val underTest = AggregatedFieldConverter(
        sequencePositionFieldConverter = SequencePositionFieldConverter(
            referenceGenomeSchema = ReferenceGenomeSchema(
                nucleotideSequences = listOf(ReferenceSequenceSchema("main")),
                genes = emptyList(),
            ),
        ),
        lineageFieldConverter = LineageFieldConverter(databaseConfig = dbConfig),
        scalarFunctionFieldConverter = ScalarFunctionFieldConverter(
            caseInsensitiveFieldsCleaner = CaseInsensitiveFieldsCleaner(dbConfig),
            databaseConfig = dbConfig,
        ),
        metadataFieldConverter = MetadataFieldConverter(
            caseInsensitiveFieldsCleaner = CaseInsensitiveFieldsCleaner(dbConfig),
        ),
    )

    @Test
    fun `convert resolves shorthand position syntax on a single-segmented genome`() {
        val result = underTest.convert("[501]")

        assertThat(result, equalTo(SequencePositionField("main", 501, isSingleSegment = true)))
    }

    @Test
    fun `convert resolves a lineage field with trailing star to a LineageWithSublineagesField`() {
        assertThat(underTest.convert("pangoLineage*"), equalTo(LineageWithSublineagesField("pangoLineage")))
    }

    @Test
    fun `convert resolves a plain lineage field without star to a PlainField`() {
        assertThat(underTest.convert("pangoLineage"), equalTo(PlainField("pangoLineage")))
    }
}
