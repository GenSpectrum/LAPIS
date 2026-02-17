package org.genspectrum.lapis.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.genspectrum.lapis.controller.MockDataCollection.DataFormat.NESTED_JSON
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.model.mutationsOverTime.MutationsOverTimeResult
import org.genspectrum.lapis.model.mutationsOverTime.QueriesOverTimeModel
import org.genspectrum.lapis.model.mutationsOverTime.QueriesOverTimeResult
import org.genspectrum.lapis.response.InfoData
import org.genspectrum.lapis.silo.DataVersion
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.security.interfaces.RSAPublicKey

@SpringBootTest(
    properties = [
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://some.value",
    ],
)
@AutoConfigureMockMvc
class OAuthTest(
    @param:Autowired val mockMvc: MockMvc,
) {
    @TestConfiguration
    class PublicJwtKeyConfig {
        @Bean
        fun jwtDecoder(): NimbusJwtDecoder = NimbusJwtDecoder.withPublicKey(keyPair.public as RSAPublicKey).build()
    }

    @MockkBean
    lateinit var siloQueryModelMock: SiloQueryModel

    @MockkBean
    lateinit var queriesOverTimeModelMock: QueriesOverTimeModel

    @MockkBean
    lateinit var dataVersion: DataVersion

    @BeforeEach
    fun setup() {
        every { dataVersion.dataVersion } returns "1234"
    }

    @ParameterizedTest(name = "GIVEN no access token WHEN I request {0} THEN returns success")
    @ValueSource(strings = ["/swagger-ui/index.html", "/api-docs", "/api-docs.yaml", "/actuator", "/actuator/caches"])
    fun `GIVEN no access token WHEN I request publicly available resource THEN returns success`(path: String) {
        mockMvc.perform(get(path))
            .andExpect(status().isOk)
    }

    @ParameterizedTest(name = "GIVEN no access token WHEN I request {0} THEN return 401 unauthorized")
    @MethodSource("getProtectedRouteScenarios")
    fun `GIVEN no access token WHEN I request THEN return 401 unauthorized`(scenario: ProtectedRouteScenario) {
        if (scenario.supportsGet) {
            mockMvc.perform(get(scenario.path))
                .andExpect(status().isUnauthorized)
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
        }

        if (scenario.supportsPost) {
            mockMvc.perform(post(scenario.path))
                .andExpect(status().isUnauthorized)
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
        }
    }

    @ParameterizedTest(name = "GIVEN invalid access token WHEN I request {0} THEN return 401 unauthorized")
    @MethodSource("getProtectedRouteScenarios")
    fun `GIVEN invalid access token WHEN I request THEN return 401 unauthorized`(scenario: ProtectedRouteScenario) {
        if (scenario.supportsGet) {
            mockMvc.perform(get(scenario.path).withAuth("invalidToken"))
                .andExpect(status().isUnauthorized)
                .andExpect(header().string("WWW-Authenticate", containsString("Bearer error=\"invalid_token\"")))
        }

        if (scenario.supportsPost) {
            mockMvc.perform(post(scenario.path).withAuth("invalidToken"))
                .andExpect(status().isUnauthorized)
                .andExpect(header().string("WWW-Authenticate", containsString("Bearer error=\"invalid_token\"")))
        }
    }

    @ParameterizedTest(name = "GIVEN valid access token WHEN I request {0} THEN returns success")
    @MethodSource("getProtectedRouteScenarios")
    fun `GIVEN valid access token WHEN I request THEN returns success`(scenario: ProtectedRouteScenario) {
        scenario.setupModelMock(siloQueryModelMock, queriesOverTimeModelMock)

        if (scenario.supportsGet) {
            mockMvc.perform(
                get(scenario.path)
                    .param("phyloTreeField", "primaryKey")
                    .withAuth(validJwt),
            )
                .andExpect(status().isOk)
        }

        if (scenario.supportsPost) {
            mockMvc.perform(
                post(scenario.path)
                    .contentType(APPLICATION_JSON)
                    .content(
                        """
                    { 
                        "phyloTreeField": "primaryKey",
                        "filters": [],
                        "queries": [],
                        "dateRanges": [],
                        "dateField": "",
                        "includeMutations": []
                    }
                        """.trimIndent(),
                    )
                    .withAuth(validJwt),
            )
                .andExpect(status().isOk)
        }
    }

    private companion object {
        @JvmStatic
        fun getProtectedRouteScenarios(): List<ProtectedRouteScenario> =
            SampleRoute.entries.map {
                ProtectedRouteScenario(
                    path = "/sample${it.pathSegment}",
                    setupModelMock = { siloQueryModelMock, _ ->
                        when (it.serveType) {
                            ServeType.SEQUENCES -> MockDataForEndpoints.sequenceEndpointMockDataForAllSequences()
                                .mockToReturnEmptyData(siloQueryModelMock)

                            ServeType.NEWICK -> MockDataForEndpoints.treeEndpointMockData()
                                .mockToReturnEmptyData(siloQueryModelMock)

                            ServeType.METADATA -> MockDataForEndpoints.getMockData(
                                it.pathSegment,
                            ).expecting(NESTED_JSON)
                                .mockToReturnEmptyData(siloQueryModelMock)
                        }
                    },
                )
            } + ProtectedRouteScenario(
                path = "/component$QUERIES_OVER_TIME_ROUTE",
                supportsGet = false,
                setupModelMock = { _, queriesOverTimeModelMock ->
                    every {
                        queriesOverTimeModelMock.evaluateQueriesOverTime(any(), any(), any(), any(), any())
                    } returns QueriesOverTimeResult(
                        queries = emptyList(),
                        dateRanges = emptyList(),
                        data = emptyList(),
                        totalCountsByDateRange = emptyList(),
                    )
                },
            ) + ProtectedRouteScenario(
                path = "/component$NUCLEOTIDE_MUTATIONS_OVER_TIME_ROUTE",
                supportsGet = false,
                setupModelMock = { _, queriesOverTimeModelMock ->
                    every {
                        queriesOverTimeModelMock.evaluateNucleotideMutations(any(), any(), any(), any(), any())
                    } returns emptyMutationsOverTimeResult
                },
            ) + ProtectedRouteScenario(
                path = "/component$AMINO_ACID_MUTATIONS_OVER_TIME_ROUTE",
                supportsGet = false,
                setupModelMock = { _, queriesOverTimeModelMock ->
                    every {
                        queriesOverTimeModelMock.evaluateAminoAcidMutations(any(), any(), any(), any(), any())
                    } returns emptyMutationsOverTimeResult
                },
            ) + ProtectedRouteScenario(
                path = "/sample$INFO_ROUTE",
                supportsPost = false,
                setupModelMock = { siloQueryModelMock, _ ->
                    every {
                        siloQueryModelMock.getInfo()
                    } returns InfoData(dataVersion = "dataVersion", siloVersion = "siloVersion")
                },
            ) + ProtectedRouteScenario(
                path = "/sample$DATABASE_CONFIG_ROUTE",
                supportsPost = false,
                setupModelMock = { _, _ -> },
            ) + ProtectedRouteScenario(
                path = "/sample$LINEAGE_DEFINITION_ROUTE/pangeLineage",
                supportsPost = false,
                setupModelMock = { siloQueryModelMock, _ ->
                    every {
                        siloQueryModelMock.getLineageDefinition(any())
                    } returns emptyMap()
                },
            ) + ProtectedRouteScenario(
                path = "/sample$REFERENCE_GENOME_ROUTE",
                supportsPost = false,
                setupModelMock = { _, _ -> },
            )
    }
}

private val emptyMutationsOverTimeResult = MutationsOverTimeResult(
    mutations = emptyList(),
    dateRanges = emptyList(),
    data = emptyList(),
    totalCountsByDateRange = emptyList(),
)

data class ProtectedRouteScenario(
    val path: String,
    val supportsGet: Boolean = true,
    val supportsPost: Boolean = true,
    val setupModelMock: (SiloQueryModel, QueriesOverTimeModel) -> Unit,
)
