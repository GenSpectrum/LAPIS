package ch.ethz.lapis.api.controller.v1;

import ch.ethz.lapis.LapisConfig;
import ch.ethz.lapis.LapisMain;
import ch.ethz.lapis.api.DataVersionService;
import ch.ethz.lapis.api.SampleService;
import ch.ethz.lapis.api.entity.OpennessLevel;
import ch.ethz.lapis.api.entity.Versioned;
import ch.ethz.lapis.api.entity.res.SampleAggregated;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(SampleController.class)
@Import({TestConfig.class, JacksonAutoConfiguration.class})
@DirtiesContext
class GisaidSampleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SampleService sampleService;

    @MockBean
    private DataVersionService dataVersionService;

    @BeforeAll
    static void setup() {
        LapisMain.globalConfig = new LapisConfig();
        LapisMain.globalConfig.setApiOpennessLevel(OpennessLevel.GISAID);
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
            .andExpect(jsonPath("$.deprecationInfo", is(nullValue())))
            .andExpect(jsonPath("$.acknowledgement", containsString("GISAID")));
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
            .andExpect(jsonPath("$.info.acknowledgement", containsString("GISAID")))
            .andExpect(jsonPath("$.data[0].count", is(count)))
            .andExpect(jsonPath("$.data[0].country", is(country)));
    }

    @Test
    void detailsEndpointIsNotAllowedInGisaidInstance() throws Exception {
        mockMvc.perform(get("/v1/sample/details")).andExpect(status().isForbidden());
    }



}
