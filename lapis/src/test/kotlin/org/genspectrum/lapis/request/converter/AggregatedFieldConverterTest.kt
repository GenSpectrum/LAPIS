package org.genspectrum.lapis.request.converter

import org.genspectrum.lapis.config.DatabaseMetadata
import org.genspectrum.lapis.config.MetadataType
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.config.ReferenceSequenceSchema
import org.genspectrum.lapis.databaseConfig
import org.genspectrum.lapis.mockActiveView
import org.genspectrum.lapis.request.SequencePositionField
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class AggregatedFieldConverterTest {
    @Test
    fun `convert resolves shorthand position syntax on a single-segmented genome`() {
        val referenceGenomeSchema = ReferenceGenomeSchema(
            nucleotideSequences = listOf(ReferenceSequenceSchema("main")),
            genes = emptyList(),
        )
        val databaseConfig = databaseConfig(
            primaryKey = "primaryKey",
            metadata = listOf(DatabaseMetadata(name = "primaryKey", type = MetadataType.STRING)),
        )
        val underTest = AggregatedFieldConverter(
            sequencePositionFieldConverter = SequencePositionFieldConverter(
                activeView = mockActiveView(referenceGenomeSchema = referenceGenomeSchema),
            ),
            metadataFieldConverter = MetadataFieldConverter(
                caseInsensitiveFieldsCleaner = CaseInsensitiveFieldsCleaner(
                    activeView = mockActiveView(databaseConfig = databaseConfig),
                ),
            ),
        )

        val result = underTest.convert("[501]")

        assertThat(result, equalTo(SequencePositionField("main", 501, isSingleSegment = true)))
    }
}
