package org.genspectrum.lapis.silo

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.URISyntaxException

class RhyDbUrisTest {
    @Test
    fun `GIVEN valid silo url THEN returns uris`() {
        val underTest = RhyDbUris("http://dummy.silo.url")

        assertThat(underTest.query.toString(), `is`("http://dummy.silo.url/query"))
        assertThat(underTest.info.toString(), `is`("http://dummy.silo.url/info"))
    }

    @Test
    fun `GIVEN invalid silo url THEN throws exception`() {
        assertThrows<URISyntaxException> {
            RhyDbUris("this is not a url")
        }
    }
}
