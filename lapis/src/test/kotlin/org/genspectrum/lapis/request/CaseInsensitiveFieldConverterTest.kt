package org.genspectrum.lapis.request

import org.genspectrum.lapis.config.DatabaseMetadata
import org.genspectrum.lapis.config.MetadataType
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.config.ReferenceSequenceSchema
import org.genspectrum.lapis.databaseConfig
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class CaseInsensitiveFieldConverterTest {
    @Test
    fun `convert resolves shorthand position syntax on a single-segmented genome`() {
        val underTest = CaseInsensitiveFieldConverter(
            caseInsensitiveFieldsCleaner = CaseInsensitiveFieldsCleaner(
                databaseConfig(
                    primaryKey = "primaryKey",
                    metadata = listOf(DatabaseMetadata(name = "primaryKey", type = MetadataType.STRING)),
                ),
            ),
            referenceGenomeSchema = ReferenceGenomeSchema(
                nucleotideSequences = listOf(ReferenceSequenceSchema("main")),
                genes = emptyList(),
            ),
        )

        val result = underTest.convert("[501]")

        assertThat(result, equalTo(SequencePositionField("main", 501, isSingleSegment = true)))
    }
}
