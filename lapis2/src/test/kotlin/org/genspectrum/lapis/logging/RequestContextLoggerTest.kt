package org.genspectrum.lapis.logging

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.http.HttpServletRequest
import org.genspectrum.lapis.util.TimeFactory
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.slf4j.Logger
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

internal class RequestContextLoggerTest {
    @MockK(relaxed = true)
    private lateinit var loggerMock: Logger

    @MockK
    private lateinit var timeFactoryMock: TimeFactory
    private lateinit var requestContext: RequestContext
    private lateinit var underTest: RequestContextLogger

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)

        requestContext = RequestContext()
        underTest = RequestContextLogger(
            requestContext,
            StatisticsLogObjectMapper(Jackson2ObjectMapperBuilder()),
            loggerMock,
            timeFactoryMock,
        )
    }

    @Test
    fun `given two timestamps then logs times`() {
        every { timeFactoryMock.now() } returnsMany listOf(100L, 199L)

        underTest.handleAndLogRequest(
            mockRequest(),
            mockk(),
            mockk(relaxed = true),
        )

        verify {
            loggerMock.info("""{"unixTimestamp":100,"responseTimeInMilliSeconds":99,"endpoint":"/shouldBeLogged"}""")
        }
    }

    @ParameterizedTest
    @MethodSource("provideInputFilters")
    fun `given an input filter then the corresponding fields are logged`(
        filter: Map<String, String>,
        expectedLogMessagePart: String?,
    ) {
        every { timeFactoryMock.now() } returns 100L
        requestContext.filter = filter

        underTest.handleAndLogRequest(
            mockRequest(),
            mockk(),
            mockk(relaxed = true),
        )

        verify {
            loggerMock.info(withArg { assertThat(it, Matchers.containsString(expectedLogMessagePart)) })
        }
    }

    companion object {
        private fun mockRequest(): HttpServletRequest {
            val httpRequestMock = mockk<HttpServletRequest>()
            every { httpRequestMock.requestURI } returns "/shouldBeLogged"
            return httpRequestMock
        }

        @JvmStatic
        fun provideInputFilters() = listOf(
            Arguments.of(
                mapOf("country" to "Germany"),
                """"country":"Germany"""",
            ),
            Arguments.of(
                mapOf("country" to "Germany", "nucleotideMutation" to "A123T"),
                """"country":"Germany","nucleotideMutation":"A123T"""",
            ),
        )
    }
}
