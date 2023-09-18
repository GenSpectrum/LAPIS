package org.genspectrum.lapis.config

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SingleSegmentedSequenceFeatureTest {
    @Test
    fun `given a databaseConfig with a feature named isSingleSegmentedSequence then isEnabled returns true`() {
        val input = databaseConfigWithFeatures(listOf(DatabaseFeature(IS_SEQUENCE_SINGLE_SEGMENTED_FEATURE)))

        val underTest = SingleSegmentedSequenceFeature(input)

        assertTrue(underTest.isEnabled())
    }

    @Test
    fun `given a databaseConfig without a feature named isSingleSegmentedSequence then isEnabled returns false`() {
        val input = databaseConfigWithFeatures(listOf(DatabaseFeature("notTheRightFeature")))

        val underTest = SingleSegmentedSequenceFeature(input)

        assertFalse(underTest.isEnabled())
    }

    private fun databaseConfigWithFeatures(
        databaseFeatures: List<DatabaseFeature> = emptyList(),
    ) = DatabaseConfig(
        DatabaseSchema(
            "test config",
            OpennessLevel.OPEN,
            emptyList(),
            "test primary key",
            databaseFeatures,
        ),
    )
}
