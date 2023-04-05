package org.genspectrum.lapis.config

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
        assertThat(underTest.schema.primaryKey, `is`("gisaid_epi_isl"))
        assertThat(
            underTest.schema.metadata,
            containsInAnyOrder(
                DatabaseMetadata(name = "gisaid_epi_isl", type = "string"),
                DatabaseMetadata(name = "date", type = "date"),
                DatabaseMetadata(name = "region", type = "string"),
                DatabaseMetadata(name = "country", type = "string"),
                DatabaseMetadata(name = "division", type = "string"),
                DatabaseMetadata(name = "pango_lineage", type = "pango_lineage"),
            ),
        )
    }
}
