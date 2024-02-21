package org.genspectrum.lapis.config

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
class AccessKeysReaderTest {
    @Autowired
    lateinit var underTest: AccessKeysReader

    @Test
    fun `given access keys file path as property then should successfully read access keys`() {
        val result = underTest.read()

        assertThat(
            result.fullAccessKeys,
            contains(
                "testFullAccessKey",
                "testFullAccessKey2",
            ),
        )

        assertThat(
            result.aggregatedDataAccessKeys,
            contains(
                "testAggregatedDataAccessKey",
                "testAggregatedDataAccessKey2",
            ),
        )
    }
}

@SpringBootTest
@ActiveProfiles("testWithoutAccessKeys")
class AccessKeysReaderWithPathNotSetTest {
    @Autowired
    lateinit var underTest: AccessKeysReader

    @Test
    fun `given access keys file path property is not set then should throw exception when reading access keys`() {
        assertThrows<IllegalArgumentException> { underTest.read() }
    }
}
