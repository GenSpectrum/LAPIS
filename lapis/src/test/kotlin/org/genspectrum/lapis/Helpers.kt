package org.genspectrum.lapis

import io.mockk.every
import io.mockk.mockk
import org.genspectrum.lapis.config.ActiveView
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.DatabaseFeature
import org.genspectrum.lapis.config.DatabaseMetadata
import org.genspectrum.lapis.config.DatabaseSchema
import org.genspectrum.lapis.config.OpennessLevel
import org.genspectrum.lapis.config.ReferenceGenome
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.config.ReferenceSequence
import org.genspectrum.lapis.config.ReferenceSequenceSchema
import org.genspectrum.lapis.config.SequenceFilterFields
import org.genspectrum.lapis.config.ViewCapability
import org.genspectrum.lapis.config.ViewConfig
import org.genspectrum.lapis.config.ViewRegistry

fun databaseConfig(
    primaryKey: String,
    metadata: List<DatabaseMetadata>,
    databaseFeatures: List<DatabaseFeature> = emptyList(),
) = DatabaseConfig(
    schema = DatabaseSchema(
        instanceName = "test",
        opennessLevel = OpennessLevel.OPEN,
        metadata = metadata,
        primaryKey = primaryKey,
        features = databaseFeatures,
    ),
)

fun mockActiveView(
    databaseConfig: DatabaseConfig = dummyDatabaseConfig,
    referenceGenomeSchema: ReferenceGenomeSchema = ReferenceGenomeSchema(
        nucleotideSequences = listOf(ReferenceSequenceSchema("main")),
        genes = emptyList(),
    ),
    referenceGenome: ReferenceGenome = referenceGenomeFromSchema(referenceGenomeSchema),
    fieldAliases: Map<String, String> = emptyMap(),
): ActiveView {
    val viewConfig = ViewConfig(
        viewName = "test",
        baseQuery = "default",
        fieldAliases = fieldAliases,
        capabilities = ViewCapability.entries.toSet(),
        databaseConfig = databaseConfig,
        referenceGenome = referenceGenome,
        referenceGenomeSchema = referenceGenomeSchema,
        sequenceFilterFields = SequenceFilterFields.fromDatabaseConfig(databaseConfig),
    )
    val activeView = mockk<ActiveView>()
    every { activeView.config } returns viewConfig
    every { activeView.databaseConfig } returns databaseConfig
    every { activeView.referenceGenome } returns referenceGenome
    every { activeView.referenceGenomeSchema } returns referenceGenomeSchema
    every { activeView.sequenceFilterFields } returns viewConfig.sequenceFilterFields
    every { activeView.resolveField(any()) } answers {
        val name = firstArg<String>()
        fieldAliases[name] ?: name
    }
    return activeView
}

fun mockViewRegistry(firstView: ViewConfig): ViewRegistry {
    val viewRegistry = mockk<ViewRegistry>()
    every { viewRegistry.first() } returns firstView
    return viewRegistry
}

fun referenceGenomeFromSchema(referenceGenomeSchema: ReferenceGenomeSchema) =
    ReferenceGenome(
        nucleotideSequences = referenceGenomeSchema.nucleotideSequences.map { ReferenceSequence(it.name, "ACGT") },
        genes = referenceGenomeSchema.genes.map { ReferenceSequence(it.name, "ACGT") },
    )
