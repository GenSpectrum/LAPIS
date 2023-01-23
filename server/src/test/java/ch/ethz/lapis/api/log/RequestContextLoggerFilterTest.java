package ch.ethz.lapis.api.log;

import ch.ethz.lapis.api.entity.AAInsertion;
import ch.ethz.lapis.api.entity.AAMutation;
import ch.ethz.lapis.api.entity.NucInsertion;
import ch.ethz.lapis.api.entity.NucMutation;
import ch.ethz.lapis.api.entity.req.SampleDetailRequest;
import ch.ethz.lapis.api.entity.req.SampleFilter;
import ch.ethz.lapis.util.TimeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

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

        underTest = new RequestContextLoggerFilter(
            requestContext,
            new StatisticsLogObjectMapper(new Jackson2ObjectMapperBuilder()),
            loggerMock,
            timeFactoryMock
        );
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
            "\"endpoint\":\"/v1/sample/shouldBeLogged\",\"returnedDataFromCache\":false}");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/UriThatShouldNotBeLogged", "/v1/sample/info"})
    void givenAHttpRequestNotToASampleRoute_thenItDoesNotLog(String uri) throws ServletException, IOException {
        when(timeFactoryMock.now()).thenReturn(LocalDateTime.now());

        underTest.doFilterInternal(getRequestTo(uri), mock(HttpServletResponse.class), mock(FilterChain.class));

        verify(loggerMock, never()).info(anyString());
    }

    @ParameterizedTest
    @MethodSource("getInputFilter")
    void givenAnInputFilter_thenTheCorrespondingFieldsAreLogged(SampleFilter filter, String expectedLogMessagePart) throws ServletException, IOException {
        when(timeFactoryMock.now()).thenReturn(LocalDateTime.now());

        requestContext.setFilter(filter);

        underTest.doFilterInternal(
            getRequestTo("/v1/sample/shouldBeLogged"),
            mock(HttpServletResponse.class),
            mock(FilterChain.class)
        );

        verify(loggerMock).info(contains(expectedLogMessagePart));
    }

    public static Stream<Arguments> getInputFilter() {
        SampleFilter aaMutationFilter = new SampleDetailRequest();
        aaMutationFilter.setAaMutations(List.of(
            new AAMutation("S", 501, 'Y')
        ));

        SampleFilter aaInsertionsFilter = new SampleDetailRequest();
        aaInsertionsFilter.setAaInsertions(List.of(
            new AAInsertion("S", 501, "EN")
        ));

        SampleFilter nucMutationFilter = new SampleDetailRequest();
        nucMutationFilter.setNucMutations(List.of(
            new NucMutation(1234, 'T')
        ));

        SampleFilter nucInsertionsFilter = new SampleDetailRequest();
        nucInsertionsFilter.setNucInsertions(List.of(
            new NucInsertion(1234, "ACT?GGT")
        ));

        SampleFilter countryFilter = new SampleDetailRequest();
        countryFilter.setCountry("Germany");

        return Stream.of(
            Arguments.of(aaMutationFilter, "\"aaMutations\":[\"S:501Y\"]"),
            Arguments.of(aaInsertionsFilter, "\"aaInsertions\":[\"ins_S:501:EN\"]"),
            Arguments.of(nucMutationFilter, "\"nucMutations\":[\"1234T\"]"),
            Arguments.of(nucInsertionsFilter, "\"nucInsertions\":[\"ins_1234:ACT?GGT\"]"),
            Arguments.of(countryFilter, "\"country\":\"Germany\"")
        );
    }
}
