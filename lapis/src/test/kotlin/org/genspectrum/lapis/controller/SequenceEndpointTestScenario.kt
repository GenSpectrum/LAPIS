package org.genspectrum.lapis.controller

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder

data class SequenceEndpointTestScenario(
    val description: String,
    val request: MockHttpServletRequestBuilder,
    val mockData: MockData,
) {
    override fun toString() = description

    companion object {
        fun createScenarios(
            route: String,
            sequenceName: String,
        ): List<SequenceEndpointTestScenario> =
            listOf(
                null,
                SequenceEndpointMockDataCollection.DataFormat.FASTA,
                SequenceEndpointMockDataCollection.DataFormat.JSON,
                SequenceEndpointMockDataCollection.DataFormat.NDJSON,
            )
                .flatMap { dataFormat ->
                    val dataFormatJsonSnippet =
                        dataFormat?.let { """, "dataFormat": "${dataFormat.fileFormat}" """ } ?: ""
                    val mockData = MockDataForEndpoints.sequenceEndpointMockData(sequenceName)
                        .expecting(dataFormat ?: SequenceEndpointMockDataCollection.DataFormat.FASTA)

                    listOf(
                        SequenceEndpointTestScenario(
                            description = "GET $route with data format $dataFormat",
                            request = getSample(route)
                                .queryParam("country", "Switzerland")
                                .apply {
                                    if (dataFormat != null) {
                                        queryParam("dataFormat", dataFormat.toString())
                                    }
                                },
                            mockData = mockData,
                        ),
                        SequenceEndpointTestScenario(
                            description = "GET $route with accept header for $dataFormat",
                            request = getSample(route)
                                .queryParam("country", "Switzerland")
                                .apply {
                                    if (dataFormat != null) {
                                        accept(dataFormat.acceptHeader)
                                    }
                                },
                            mockData = mockData,
                        ),
                        SequenceEndpointTestScenario(
                            description = "POST JSON $route with data format $dataFormat",
                            request = postSample(route)
                                .content("""{"country": "Switzerland" $dataFormatJsonSnippet}""")
                                .contentType(MediaType.APPLICATION_JSON),
                            mockData = mockData,
                        ),
                        SequenceEndpointTestScenario(
                            description = "POST JSON $route with accept header for $dataFormat",
                            request = postSample(route)
                                .content("""{"country": "Switzerland"}""")
                                .contentType(MediaType.APPLICATION_JSON)
                                .apply {
                                    if (dataFormat != null) {
                                        accept(dataFormat.acceptHeader)
                                    }
                                },
                            mockData = mockData,
                        ),
                        SequenceEndpointTestScenario(
                            description = "POST form encoded $route with data format $dataFormat",
                            request = postSample(route)
                                .param("country", "Switzerland")
                                .apply {
                                    if (dataFormat != null) {
                                        param("dataFormat", dataFormat.toString())
                                    }
                                }
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE),
                            mockData = mockData,
                        ),
                        SequenceEndpointTestScenario(
                            description = "POST form encoded $route with accept header for $dataFormat",
                            request = postSample(route)
                                .param("country", "Switzerland")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                                .apply {
                                    if (dataFormat != null) {
                                        accept(dataFormat.acceptHeader)
                                    }
                                },
                            mockData = mockData,
                        ),
                    )
                }
    }
}
