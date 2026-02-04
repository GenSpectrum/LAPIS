package org.genspectrum.lapis.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.genspectrum.lapis.controller.MockDataCollection.DataFormat.NESTED_JSON
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.silo.DataVersion
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.empty
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.security.interfaces.RSAPublicKey

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

    @MockkBean
    lateinit var dataVersion: DataVersion

    @BeforeEach
    fun setup() {
        every {
            dataVersion.dataVersion
        } returns "1234"
    }

    @ParameterizedTest(name = "GIVEN no access token WHEN I request {0} THEN return 401 unauthorized")
    @MethodSource("getScenarios")
    fun `GIVEN no access token WHEN I request THEN return 401 unauthorized`(route: SampleRoute) {
        mockMvc.perform(get("/sample${route.pathSegment}"))
            .andExpect(status().isUnauthorized)
            .andExpect(header().string("WWW-Authenticate", "Bearer"))

        mockMvc.perform(post("/sample${route.pathSegment}"))
            .andExpect(status().isForbidden)
            .andExpect(content().string(""))
    }

    @ParameterizedTest(name = "GIVEN invalid access token WHEN I request {0} THEN return 401 unauthorized")
    @MethodSource("getScenarios")
    fun `GIVEN invalid access token WHEN I request THEN return 401 unauthorized`(route: SampleRoute) {
        mockMvc.perform(get("/sample${route.pathSegment}").withAuth("invalidToken"))
            .andExpect(status().isUnauthorized)
            .andExpect(header().string("WWW-Authenticate", containsString("Bearer error=\"invalid_token\"")))

        mockMvc.perform(post("/sample${route.pathSegment}").withAuth("invalidToken"))
            .andExpect(status().isUnauthorized)
            .andExpect(header().string("WWW-Authenticate", containsString("Bearer error=\"invalid_token\"")))
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getScenarios")
    fun `TODO`(route: SampleRoute) {
        when (route.serveType) {
            ServeType.SEQUENCES -> TODO()

            ServeType.NEWICK -> TODO()

            ServeType.METADATA -> MockDataForEndpoints.getMockData(route.pathSegment).expecting(NESTED_JSON)
                .mockToReturnEmptyData(siloQueryModelMock)
        }

        mockMvc.perform(
            get("/sample${route.pathSegment}")
                .param("phyloTreeField", "primaryKey")
                .withAuth(validJwt),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data").value(empty<Any>()))

        mockMvc.perform(
            post("/sample${route.pathSegment}")
                .contentType(APPLICATION_JSON)
                .content("""{ "phyloTreeField": "primaryKey" }""")
                .withAuth(validJwt),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data").value(empty<Any>()))
    }

    private companion object {
        @JvmStatic
        fun getScenarios() = SampleRoute.entries
    }

    @TestConfiguration
    class PublicJwtKeyConfig {
        @Bean
        fun jwtDecoder(): NimbusJwtDecoder = NimbusJwtDecoder.withPublicKey(keyPair.public as RSAPublicKey).build()
    }
}
