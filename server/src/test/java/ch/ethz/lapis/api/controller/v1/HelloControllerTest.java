package ch.ethz.lapis.api.controller.v1;

import ch.ethz.lapis.api.SampleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(HelloController.class)
@Import({TestConfig.class})
class HelloControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SampleService service;


    @Test
    void emtpyRequest() throws Exception {
        mockMvc.perform(get("/v1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message", containsString("LAPIS")));
    }

    @Test
    void nextcladeDataset() throws Exception {
        when(service.getNextcladeDatasetTag()).thenReturn("nextcladeDataSet");

        mockMvc.perform(get("/v1/info/nextclade-dataset"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name", is("sars-cov-2")))
            .andExpect(jsonPath("$.tag", is("nextcladeDataSet")));
    }
}
