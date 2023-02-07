package ch.ethz.lapis.api.controller.v1;

import ch.ethz.lapis.LapisConfig;
import ch.ethz.lapis.LapisMain;
import ch.ethz.lapis.api.DataVersionService;
import ch.ethz.lapis.api.SampleService;
import ch.ethz.lapis.api.entity.OpennessLevel;
import ch.ethz.lapis.api.entity.Versioned;
import ch.ethz.lapis.api.entity.res.SampleAggregated;
import ch.ethz.lapis.api.entity.res.SampleDetail;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(SampleController.class)
@Import({TestConfig.class, JacksonAutoConfiguration.class})
@DirtiesContext
class OpenSampleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SampleService sampleService;

    @MockBean
    private DataVersionService dataVersionService;

    @BeforeAll
    static void setup() {
        LapisMain.globalConfig = new LapisConfig();
        LapisMain.globalConfig.setApiOpennessLevel(OpennessLevel.OPEN);
    }

    @Test
    void info() throws Exception {
        long dataVersion = 1;
        when(dataVersionService.getVersion()).thenReturn(dataVersion);

        mockMvc.perform(get("/v1/sample/info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.apiVersion", is(1)))
            .andExpect(jsonPath("$.dataVersion", is((int) dataVersion)))
            .andExpect(jsonPath("$.deprecationDate", is(nullValue())))
            .andExpect(jsonPath("$.deprecationInfo", is(nullValue())));
    }

    @Test
    void aggregated() throws Exception {
        long dataVersion = 1;
        SampleAggregated testSample = new SampleAggregated();
        int count = 5;
        testSample.setCount(count);
        String country = "Germany";
        testSample.setCountry(country);
        when(sampleService.getAggregatedSamples(any())).thenReturn(new Versioned<>(dataVersion, List.of(testSample)));

        mockMvc.perform(get("/v1/sample/aggregated?fields=country"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors", emptyIterable()))
            .andExpect(jsonPath("$.info.apiVersion", is(1)))
            .andExpect(jsonPath("$.info.dataVersion", is((int) dataVersion)))
            .andExpect(jsonPath("$.info.deprecationDate", is(nullValue())))
            .andExpect(jsonPath("$.info.deprecationInfo", is(nullValue())))
            .andExpect(jsonPath("$.data[0].count", is(count)))
            .andExpect(jsonPath("$.data[0].country", is(country)));
    }

    @Test
    void detailsEndpointIsAllowedInOpenInstance() throws Exception {
        long dataVersion = 1;
        when(dataVersionService.getVersion()).thenReturn(dataVersion);

        when(sampleService.getDetailedSamples(any(), any())).thenReturn(List.of(new SampleDetail()));

        mockMvc.perform(get("/v1/sample/details"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors", emptyIterable()))
            .andExpect(jsonPath("$.info.apiVersion", is(1)))
            .andExpect(jsonPath("$.info.dataVersion", is((int) dataVersion)))
            .andExpect(jsonPath("$.info.deprecationDate", is(nullValue())))
            .andExpect(jsonPath("$.info.deprecationInfo", is(nullValue())))
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath(
                "$.data[0]",
                allOf(Arrays.stream(SampleDetail.Fields.values())
                    .map(field -> hasEntry(field.toString(), null))
                    .collect(toList()))
            ));
    }

    @Test
    void detailsEndpointFiltersOutputByGivenFields() throws Exception {
        long dataVersion = 1;
        when(dataVersionService.getVersion()).thenReturn(dataVersion);

        String country = "Germany";
        int age = 123;
        List<SampleDetail> testSample = List.of(
            new SampleDetail().setCountryExposure(country).setAge(age),
            new SampleDetail().setAge(age),
            new SampleDetail().setCountryExposure(country),
            new SampleDetail()
        );

        when(sampleService.getDetailedSamples(any(), any())).thenReturn(testSample);

        mockMvc.perform(get("/v1/sample/details?fields=countryExposure,age"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors", emptyIterable()))
            .andExpect(jsonPath("$.data", hasSize(4)))
            .andExpect(jsonPath("$.data[0]", isObjectWithExactlyCountryExposureAndAgeFields(country, age)))
            .andExpect(jsonPath("$.data[1]", isObjectWithExactlyCountryExposureAndAgeFields(null, age)))
            .andExpect(jsonPath("$.data[2]", isObjectWithExactlyCountryExposureAndAgeFields(country, null)))
            .andExpect(jsonPath("$.data[3]", isObjectWithExactlyCountryExposureAndAgeFields(null, null)));
    }


    private static Matcher<Map<? extends String, ?>> isObjectWithExactlyCountryExposureAndAgeFields(String countryExposure, Object age) {
        return allOf(
            hasEntry("age", age),
            hasEntry("countryExposure", countryExposure),
            aMapWithSize(2)
        );
    }

    @Test
    void detailsEndpointFiltersCsvOutputByGivenFields() throws Exception {
        long dataVersion = 1;
        when(dataVersionService.getVersion()).thenReturn(dataVersion);

        String country = "Germany";
        int age = 123;
        List<SampleDetail> testSample = List.of(
            new SampleDetail().setCountryExposure(country).setAge(age)
        );

        when(sampleService.getDetailedSamples(any(), any())).thenReturn(testSample);

        mockMvc.perform(get("/v1/sample/details?fields=age,countryExposure&dataFormat=csv"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("text/csv"))
            .andExpect(content().string("age,countryExposure\r\n123,Germany\r\n"));
    }

    @Test
    void detailsEndpointReturnsCsvWithAllFields() throws Exception {
        long dataVersion = 1;
        when(dataVersionService.getVersion()).thenReturn(dataVersion);

        when(sampleService.getDetailedSamples(any(), any())).thenReturn(List.of(new SampleDetail()));

        String result = mockMvc.perform(get("/v1/sample/details?dataFormat=csv"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("text/csv"))
            .andReturn().getResponse().getContentAsString();

        var rows = result.split("\r\n");
        assertThat(
            rows[0],
            allOf(
                Arrays.stream(SampleDetail.Fields.values())
                    .map(field -> containsString(field.toString()))
                    .collect(toList())
            )
        );
        assertThat(rows[1], is(",".repeat(SampleDetail.Fields.values().length - 1)));
    }

    @Test
    void detailsEndpointFiltersTsvOutputByGivenFields() throws Exception {
        long dataVersion = 1;
        when(dataVersionService.getVersion()).thenReturn(dataVersion);

        String country = "Germany";
        int age = 123;
        List<SampleDetail> testSample = List.of(
            new SampleDetail().setCountryExposure(country).setAge(age)
        );

        when(sampleService.getDetailedSamples(any(), any())).thenReturn(testSample);

        mockMvc.perform(get("/v1/sample/details?fields=age,countryExposure&dataFormat=tsv"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("text/plain"))
            .andExpect(content().string("age\tcountryExposure\r\n123\tGermany\r\n"));
    }

    @Test
    void detailsEndpointReturnsTsvWithAllFields() throws Exception {
        long dataVersion = 1;
        when(dataVersionService.getVersion()).thenReturn(dataVersion);

        when(sampleService.getDetailedSamples(any(), any())).thenReturn(List.of(new SampleDetail()));

        String result = mockMvc.perform(get("/v1/sample/details?dataFormat=tsv"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("text/plain"))
            .andReturn().getResponse().getContentAsString();

        var rows = result.split("\r\n");
        assertThat(
            rows[0],
            allOf(
                Arrays.stream(SampleDetail.Fields.values())
                    .map(field -> containsString(field.toString()))
                    .collect(toList())
            )
        );
        assertThat(rows[1], is("\t".repeat(SampleDetail.Fields.values().length - 1)));
    }

}
