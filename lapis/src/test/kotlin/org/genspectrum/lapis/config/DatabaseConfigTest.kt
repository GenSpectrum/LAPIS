package org.genspectrum.lapis.config

import org.genspectrum.lapis.PRIMARY_KEY_FIELD
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class DatabaseConfigTest {
    @Autowired
    private lateinit var underTest: DatabaseConfig

    @Test
    fun `load test database config`() {
        assertThat(underTest.schema.instanceName, `is`("sars_cov-2_minimal_test_config"))
        assertThat(underTest.schema.primaryKey, `is`(PRIMARY_KEY_FIELD))
        assertThat(underTest.schema.opennessLevel, `is`(OpennessLevel.OPEN))
        assertThat(
            underTest.schema.metadata,
            containsInAnyOrder(
                DatabaseMetadata(name = PRIMARY_KEY_FIELD, type = MetadataType.STRING, isPhyloTreeField = true),
                DatabaseMetadata(name = "date", type = MetadataType.DATE),
                DatabaseMetadata(name = "region", type = MetadataType.STRING),
                DatabaseMetadata(name = "country", type = MetadataType.STRING),
                DatabaseMetadata(name = "pangoLineage", type = MetadataType.STRING, generateLineageIndex = true),
                DatabaseMetadata(name = "test_boolean_column", type = MetadataType.BOOLEAN),
                DatabaseMetadata(name = "age", type = MetadataType.INT),
                DatabaseMetadata(name = "floatValue", type = MetadataType.FLOAT),
            ),
        )
        assertThat(
            underTest.schema.features,
            containsInAnyOrder(
                DatabaseFeature(name = SARS_COV2_VARIANT_QUERY_FEATURE),
                DatabaseFeature(name = GENERALIZED_ADVANCED_QUERY_FEATURE),
            ),
        )
    }
}

@SpringBootTest(
    properties = ["lapis.databaseConfig.path=src/test/resources/config/testDatabaseConfigWithoutFeatures.yaml"],
)
class DatabaseConfigWithoutFeaturesTest {
    @Autowired
    private lateinit var underTest: DatabaseConfig

    @Test
    fun `a config without features can be read`() {
        assertThat(underTest.schema.instanceName, `is`("sars_cov-2_minimal_test_config"))
        assertThat(underTest.schema.primaryKey, `is`(PRIMARY_KEY_FIELD))
        assertThat(
            underTest.schema.metadata,
            containsInAnyOrder(
                DatabaseMetadata(name = PRIMARY_KEY_FIELD, type = MetadataType.STRING),
                DatabaseMetadata(name = "date", type = MetadataType.DATE),
                DatabaseMetadata(name = "region", type = MetadataType.STRING),
                DatabaseMetadata(name = "country", type = MetadataType.STRING),
                DatabaseMetadata(name = "pangoLineage", type = MetadataType.STRING, generateLineageIndex = true),
            ),
        )
        assertThat(underTest.schema.features, `is`(emptyList()))
    }
}
