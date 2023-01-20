package ch.ethz.lapis.api.log;

import ch.ethz.lapis.api.entity.req.SampleDetailRequest;
import ch.ethz.lapis.api.entity.req.SampleFilter;
import ch.ethz.lapis.util.TimeFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestContextLoggerFilterTest {
    private Logger loggerMock;
    private TimeFactory timeFactoryMock;
    private RequestContextLoggerFilter underTest;
    private RequestContext requestContext;

    private static HttpServletRequest getRequestTo(String uri) {
        HttpServletRequest httpRequestMock = mock(HttpServletRequest.class);
        when(httpRequestMock.getRequestURI()).thenReturn(uri);
        return httpRequestMock;
    }

    @BeforeEach
    void setup() {
        loggerMock = mock(Logger.class);
        timeFactoryMock = mock(TimeFactory.class);

        requestContext = new RequestContext();

        underTest = new RequestContextLoggerFilter(requestContext, new StatisticsLogObjectMapper(), loggerMock, timeFactoryMock);
    }

    @Test
    void givenTwoTimestamps_thenLogsTimes() throws ServletException, IOException {
        LocalDateTime timeBefore = LocalDateTime.parse("2007-12-03T10:15:30.001");
        LocalDateTime timeAfter = LocalDateTime.parse("2007-12-03T10:15:30.100");

        when(timeFactoryMock.now()).thenReturn(timeBefore, timeAfter);

        underTest.doFilterInternal(
            getRequestTo("/v1/sample/shouldBeLogged"),
            mock(HttpServletResponse.class),
            mock(FilterChain.class)
        );

        verify(loggerMock).info("{\"timestamp\":\"2007-12-03T10:15:30.001\",\"responseTimeInSeconds\":0.099000000," +
            "\"endpoint\":\"/v1/sample/shouldBeLogged\",\"returnedDataFromCache\":false,\"variantFilter\":null}");
    }

    @Test
    void givenAVariantFilter_thenLogsFilter() throws ServletException, IOException {
        when(timeFactoryMock.now()).thenReturn(LocalDateTime.now());

        SampleFilter inputFilter = new SampleDetailRequest();
        inputFilter.setCountry("Germany");
        requestContext.setVariantFilter(inputFilter);

        underTest.doFilterInternal(
            getRequestTo("/v1/sample/shouldBeLogged"),
            mock(HttpServletResponse.class),
            mock(FilterChain.class)
        );

        verify(loggerMock).info(contains("\"country\":\"Germany\""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/UriThatShouldNotBeLogged", "/v1/sample/info"})
    void givenAHttpRequestNotToASampleRoute_thenItDoesNotLog(String uri) throws ServletException, IOException {
        when(timeFactoryMock.now()).thenReturn(LocalDateTime.now());

        underTest.doFilterInternal(getRequestTo(uri), mock(HttpServletResponse.class), mock(FilterChain.class));

        verify(loggerMock, never()).info(anyString());
    }
}
