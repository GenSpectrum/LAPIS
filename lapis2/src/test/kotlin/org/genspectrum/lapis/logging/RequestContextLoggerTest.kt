package org.genspectrum.lapis.logging

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.genspectrum.lapis.request.CommonSequenceFilters
import org.genspectrum.lapis.request.SequenceFiltersRequestWithFields
import org.genspectrum.lapis.util.TimeFactory
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
internal class RequestContextLoggerTest {
    @MockK(relaxed = true)
    private lateinit var loggerMock: Logger

    @MockK
    private lateinit var timeFactoryMock: TimeFactory

    @Autowired
    private lateinit var statisticsLogObjectMapper: StatisticsLogObjectMapper

    @Autowired
    private lateinit var requestContext: RequestContext

    private lateinit var underTest: RequestContextLogger

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)

        underTest = RequestContextLogger(
            requestContext,
            statisticsLogObjectMapper,
            loggerMock,
            timeFactoryMock,
        )
    }

    @Test
    fun `given two timestamps then logs times`() {
        every { timeFactoryMock.now() } returnsMany listOf(100L, 199L)

        underTest.handleAndLogRequest(
            mockRequest(),
            mockResponse(),
            mockk(relaxed = true),
        )

        verify {
            loggerMock.info(
                """
                {"unixTimestamp":100,"responseTimeInMilliSeconds":99,"endpoint":"/shouldBeLogged","responseCode":200}
                """.trimIndent(),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("provideInputFilters")
    fun `given an input filter then the corresponding fields are logged`(
        filter: CommonSequenceFilters,
        expectedLogMessagePart: String?,
    ) {
        every { timeFactoryMock.now() } returns 100L
        requestContext.filter = filter

        underTest.handleAndLogRequest(
            mockRequest(),
            mockResponse(),
            mockk(relaxed = true),
        )

        verify {
            loggerMock.info(withArg { assertThat(it, Matchers.containsString(expectedLogMessagePart)) })
        }
    }

    private fun mockRequest(): HttpServletRequest {
        val httpRequestMock = mockk<HttpServletRequest>()
        every { httpRequestMock.requestURI } returns "/shouldBeLogged"
        return httpRequestMock
    }

    private fun mockResponse(): HttpServletResponse {
        val httpRequestMock = mockk<HttpServletResponse>()
        every { httpRequestMock.status } returns 200
        return httpRequestMock
    }

    companion object {

        @JvmStatic
        fun provideInputFilters() = listOf(
            Arguments.of(
                SequenceFiltersRequestWithFields(
                    mapOf("country" to "Germany"),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                ),
                """"country":"Germany"""",
            ),
            Arguments.of(
                SequenceFiltersRequestWithFields(
                    mapOf("country" to "Germany", "nucleotideMutation" to "A123T"),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                ),
                """"country":"Germany","nucleotideMutation":"A123T"""",
            ),
        )
    }
}
