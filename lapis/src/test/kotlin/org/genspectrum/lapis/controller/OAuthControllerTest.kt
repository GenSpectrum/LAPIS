package org.genspectrum.lapis.controller

import com.ninjasquad.springmockk.MockkBean
import org.genspectrum.lapis.model.SiloQueryModel
import org.hamcrest.Matchers
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(
    properties = [
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://some.value",
    ],
)
@AutoConfigureMockMvc
class OAuthControllerTest(
    @param:Autowired val mockMvc: MockMvc,
) {
    @MockkBean
    lateinit var siloQueryModelMock: SiloQueryModel

//    @MockkBean
//    lateinit var dataVersion: DataVersion
//
//    @BeforeEach
//    fun setup() {
//        every {
//            dataVersion.dataVersion
//        } returns "1234"
//    }

    @ParameterizedTest(name = "GIVEN no access token WHEN I request {0} THEN return 401 unauthorized")
    @MethodSource("getScenarios")
    fun `GIVEN no access token WHEN I request THEN return 401 unauthorized`(route: SampleRoute) {
        mockMvc.perform(get("/sample/${route.pathSegment}"))
            .andExpect(status().isUnauthorized)
            .andExpect(header().string("WWW-Authenticate", "Bearer"))
    }

    private companion object {
        @JvmStatic
        fun getScenarios() = SampleRoute.entries
    }
}
