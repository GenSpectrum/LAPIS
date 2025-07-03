package org.genspectrum.lapis.controller

import com.fasterxml.jackson.databind.node.TextNode
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.genspectrum.lapis.config.REFERENCE_GENOME_GENES_APPLICATION_ARG_PREFIX
import org.genspectrum.lapis.config.REFERENCE_GENOME_SEGMENTS_APPLICATION_ARG_PREFIX
import org.genspectrum.lapis.request.FASTA_HEADER_TEMPLATE_PROPERTY
import org.genspectrum.lapis.response.SequenceData
import org.genspectrum.lapis.silo.DataVersion
import org.genspectrum.lapis.silo.SiloClient
import org.genspectrum.lapis.silo.SiloQuery
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.stream.Stream

private const val SEGMENT_NAME = "otherSegment"
private const val GENE_NAME = "gene1"

@SpringBootTest(
    properties = [
        "$REFERENCE_GENOME_SEGMENTS_APPLICATION_ARG_PREFIX=someSegment,$SEGMENT_NAME",
        "$REFERENCE_GENOME_GENES_APPLICATION_ARG_PREFIX=$GENE_NAME,gene2",
    ],
)
@AutoConfigureMockMvc
class LapisControllerFastaHeaderTemplateTest(
    @param:Autowired val mockMvc: MockMvc,
) {
    @MockkBean
    lateinit var siloClient: SiloClient

    @MockkBean
    lateinit var dataVersion: DataVersion

    @BeforeEach
    fun setup() {
        every {
            dataVersion.dataVersion
        } returns "1234"
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getNucleotideFastaHeaderScenarios")
    fun `nucleotide fasta header templates`(scenario: FastaHeaderRequestScenario) {
        every {
            siloClient.sendQuery(query = any<SiloQuery<SequenceData>>())
        } returns Stream.of(
            SequenceData(mapOf(SEGMENT_NAME to TextNode("AAAA"), "primaryKey" to TextNode("1234"))),
            SequenceData(mapOf(SEGMENT_NAME to TextNode("GGGG"), "primaryKey" to TextNode("5678"))),
        )

        mockMvc.perform(scenario.request)
            .andExpect(status().isOk)
            .andExpect(content().string(scenario.expectedFasta))
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getAminoAcidFastaHeaderScenarios")
    fun `amino acid fasta header templates`(scenario: FastaHeaderRequestScenario) {
        every {
            siloClient.sendQuery(query = any<SiloQuery<SequenceData>>())
        } returns Stream.of(
            SequenceData(mapOf(GENE_NAME to TextNode("AAAA"), "primaryKey" to TextNode("1234"))),
            SequenceData(mapOf(GENE_NAME to TextNode("GGGG"), "primaryKey" to TextNode("5678"))),
        )

        mockMvc.perform(scenario.request)
            .andExpect(status().isOk)
            .andExpect(content().string(scenario.expectedFasta))
    }

    private companion object {
        const val NUCLEOTIDE_TEMPLATE = "segment={.segment}|primaryKey={primaryKey}"

        val expectedTemplatedNucleotideFasta = """
            >segment=$SEGMENT_NAME|primaryKey=1234
            AAAA
            >segment=$SEGMENT_NAME|primaryKey=5678
            GGGG
            
        """.trimIndent()

        @JvmStatic
        fun getNucleotideFastaHeaderScenarios() =
            listOf(
                FastaHeaderRequestScenario(
                    description = "GET all aligned nucleotide sequences with header template",
                    request = getSample(ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE)
                        .param(FASTA_HEADER_TEMPLATE_PROPERTY, NUCLEOTIDE_TEMPLATE),
                    expectedFastaHeaderTemplate = NUCLEOTIDE_TEMPLATE,
                    expectedFasta = expectedTemplatedNucleotideFasta,
                ),
                FastaHeaderRequestScenario(
                    description = "GET all aligned nucleotide sequences without explicit header template",
                    request = getSample(ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE),
                    expectedFastaHeaderTemplate = "{primaryKey}|{.segment}",
                    expectedFasta = """
                        >1234|$SEGMENT_NAME
                        AAAA
                        >5678|$SEGMENT_NAME
                        GGGG
                        
                    """.trimIndent(),
                ),
                FastaHeaderRequestScenario(
                    description = "POST all aligned nucleotide sequences with header template",
                    request = postSample(ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE)
                        .contentType(APPLICATION_JSON)
                        .content("""{ "$FASTA_HEADER_TEMPLATE_PROPERTY": "$NUCLEOTIDE_TEMPLATE" }"""),
                    expectedFastaHeaderTemplate = NUCLEOTIDE_TEMPLATE,
                    expectedFasta = expectedTemplatedNucleotideFasta,
                ),
                FastaHeaderRequestScenario(
                    description = "POST all aligned nucleotide sequences without explicit header template",
                    request = postSample(ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE)
                        .contentType(APPLICATION_JSON)
                        .content("{}"),
                    expectedFastaHeaderTemplate = "{primaryKey}|{.segment}",
                    expectedFasta = """
                        >1234|$SEGMENT_NAME
                        AAAA
                        >5678|$SEGMENT_NAME
                        GGGG
                        
                    """.trimIndent(),
                ),
                FastaHeaderRequestScenario(
                    description = "GET aligned nucleotide sequences with header template",
                    request = getSample("$ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE/$SEGMENT_NAME")
                        .param(FASTA_HEADER_TEMPLATE_PROPERTY, NUCLEOTIDE_TEMPLATE),
                    expectedFastaHeaderTemplate = NUCLEOTIDE_TEMPLATE,
                    expectedFasta = expectedTemplatedNucleotideFasta,
                ),
                FastaHeaderRequestScenario(
                    description = "GET aligned nucleotide sequences without explicit header template",
                    request = getSample("$ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE/$SEGMENT_NAME"),
                    expectedFastaHeaderTemplate = "{primaryKey}",
                    expectedFasta = """
                        >1234
                        AAAA
                        >5678
                        GGGG
                        
                    """.trimIndent(),
                ),
                FastaHeaderRequestScenario(
                    description = "POST aligned nucleotide sequences with header template",
                    request = postSample("$ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE/$SEGMENT_NAME")
                        .contentType(APPLICATION_JSON)
                        .content("""{ "$FASTA_HEADER_TEMPLATE_PROPERTY": "$NUCLEOTIDE_TEMPLATE" }"""),
                    expectedFastaHeaderTemplate = NUCLEOTIDE_TEMPLATE,
                    expectedFasta = expectedTemplatedNucleotideFasta,
                ),
                FastaHeaderRequestScenario(
                    description = "POST aligned nucleotide sequences without explicit header template",
                    request = postSample("$ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE/$SEGMENT_NAME")
                        .contentType(APPLICATION_JSON)
                        .content("{}"),
                    expectedFastaHeaderTemplate = "{primaryKey}",
                    expectedFasta = """
                        >1234
                        AAAA
                        >5678
                        GGGG
                        
                    """.trimIndent(),
                ),


                FastaHeaderRequestScenario(
                    description = "GET all unaligned nucleotide sequences with header template",
                    request = getSample(UNALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE)
                        .param(FASTA_HEADER_TEMPLATE_PROPERTY, NUCLEOTIDE_TEMPLATE),
                    expectedFastaHeaderTemplate = NUCLEOTIDE_TEMPLATE,
                    expectedFasta = expectedTemplatedNucleotideFasta,
                ),
                FastaHeaderRequestScenario(
                    description = "GET all unaligned nucleotide sequences without explicit header template",
                    request = getSample(UNALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE),
                    expectedFastaHeaderTemplate = "{primaryKey}|{.segment}",
                    expectedFasta = """
                        >1234|$SEGMENT_NAME
                        AAAA
                        >5678|$SEGMENT_NAME
                        GGGG
                        
                    """.trimIndent(),
                ),
                FastaHeaderRequestScenario(
                    description = "POST all unaligned nucleotide sequences with header template",
                    request = postSample(UNALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE)
                        .contentType(APPLICATION_JSON)
                        .content("""{ "$FASTA_HEADER_TEMPLATE_PROPERTY": "$NUCLEOTIDE_TEMPLATE" }"""),
                    expectedFastaHeaderTemplate = NUCLEOTIDE_TEMPLATE,
                    expectedFasta = expectedTemplatedNucleotideFasta,
                ),
                FastaHeaderRequestScenario(
                    description = "POST all unaligned nucleotide sequences without explicit header template",
                    request = postSample(UNALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE)
                        .contentType(APPLICATION_JSON)
                        .content("{}"),
                    expectedFastaHeaderTemplate = "{primaryKey}|{.segment}",
                    expectedFasta = """
                        >1234|$SEGMENT_NAME
                        AAAA
                        >5678|$SEGMENT_NAME
                        GGGG
                        
                    """.trimIndent(),
                ),
                FastaHeaderRequestScenario(
                    description = "GET unaligned nucleotide sequences with header template",
                    request = getSample("$UNALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE/$SEGMENT_NAME")
                        .param(FASTA_HEADER_TEMPLATE_PROPERTY, NUCLEOTIDE_TEMPLATE),
                    expectedFastaHeaderTemplate = NUCLEOTIDE_TEMPLATE,
                    expectedFasta = expectedTemplatedNucleotideFasta,
                ),
                FastaHeaderRequestScenario(
                    description = "GET unaligned nucleotide sequences without explicit header template",
                    request = getSample("$UNALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE/$SEGMENT_NAME"),
                    expectedFastaHeaderTemplate = "{primaryKey}",
                    expectedFasta = """
                        >1234
                        AAAA
                        >5678
                        GGGG
                        
                    """.trimIndent(),
                ),
                FastaHeaderRequestScenario(
                    description = "POST unaligned nucleotide sequences with header template",
                    request = postSample("$UNALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE/$SEGMENT_NAME")
                        .contentType(APPLICATION_JSON)
                        .content("""{ "$FASTA_HEADER_TEMPLATE_PROPERTY": "$NUCLEOTIDE_TEMPLATE" }"""),
                    expectedFastaHeaderTemplate = NUCLEOTIDE_TEMPLATE,
                    expectedFasta = expectedTemplatedNucleotideFasta,
                ),
                FastaHeaderRequestScenario(
                    description = "POST unaligned nucleotide sequences without explicit header template",
                    request = postSample("$UNALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE/$SEGMENT_NAME")
                        .contentType(APPLICATION_JSON)
                        .content("{}"),
                    expectedFastaHeaderTemplate = "{primaryKey}",
                    expectedFasta = """
                        >1234
                        AAAA
                        >5678
                        GGGG
                        
                    """.trimIndent(),
                ),
            )

        const val GENE_TEMPLATE = "gene={.gene}|primaryKey={primaryKey}"

        val expectedTemplatedAminoAcidFasta = """
            >gene=$GENE_NAME|primaryKey=1234
            AAAA
            >gene=$GENE_NAME|primaryKey=5678
            GGGG
            
        """.trimIndent()

        @JvmStatic
        val aminoAcidFastaHeaderScenarios = listOf(
            FastaHeaderRequestScenario(
                description = "GET all amino acid sequences with header template",
                request = getSample(ALIGNED_AMINO_ACID_SEQUENCES_ROUTE)
                    .param(FASTA_HEADER_TEMPLATE_PROPERTY, GENE_TEMPLATE),
                expectedFastaHeaderTemplate = GENE_TEMPLATE,
                expectedFasta = expectedTemplatedAminoAcidFasta,
            ),
            FastaHeaderRequestScenario(
                description = "GET all amino acid sequences without explicit header template",
                request = getSample(ALIGNED_AMINO_ACID_SEQUENCES_ROUTE),
                expectedFastaHeaderTemplate = "{primaryKey}|{.gene}",
                expectedFasta = """
                    >1234|$GENE_NAME
                    AAAA
                    >5678|$GENE_NAME
                    GGGG
                    
                """.trimIndent(),
            ),
            FastaHeaderRequestScenario(
                description = "POST all amino acid sequences with header template",
                request = postSample(ALIGNED_AMINO_ACID_SEQUENCES_ROUTE)
                    .contentType(APPLICATION_JSON)
                    .content("""{ "$FASTA_HEADER_TEMPLATE_PROPERTY": "$GENE_TEMPLATE" }"""),
                expectedFastaHeaderTemplate = GENE_TEMPLATE,
                expectedFasta = expectedTemplatedAminoAcidFasta,
            ),
            FastaHeaderRequestScenario(
                description = "POST all amino acid sequences without explicit header template",
                request = postSample(ALIGNED_AMINO_ACID_SEQUENCES_ROUTE)
                    .contentType(APPLICATION_JSON)
                    .content("{}"),
                expectedFastaHeaderTemplate = "{primaryKey}|{.gene}",
                expectedFasta = """
                    >1234|$GENE_NAME
                    AAAA
                    >5678|$GENE_NAME
                    GGGG
                    
                """.trimIndent(),
            ),
            FastaHeaderRequestScenario(
                description = "GET amino acid sequences with header template",
                request = getSample("$ALIGNED_AMINO_ACID_SEQUENCES_ROUTE/$GENE_NAME")
                    .param(FASTA_HEADER_TEMPLATE_PROPERTY, GENE_TEMPLATE),
                expectedFastaHeaderTemplate = GENE_TEMPLATE,
                expectedFasta = expectedTemplatedAminoAcidFasta,
            ),
            FastaHeaderRequestScenario(
                description = "GET amino acid sequences without explicit header template",
                request = getSample("$ALIGNED_AMINO_ACID_SEQUENCES_ROUTE/$GENE_NAME"),
                expectedFastaHeaderTemplate = "{primaryKey}",
                expectedFasta = """
                    >1234
                    AAAA
                    >5678
                    GGGG
                    
                """.trimIndent(),
            ),
            FastaHeaderRequestScenario(
                description = "POST amino acid sequences with header template",
                request = postSample("$ALIGNED_AMINO_ACID_SEQUENCES_ROUTE/$GENE_NAME")
                    .contentType(APPLICATION_JSON)
                    .content("""{ "$FASTA_HEADER_TEMPLATE_PROPERTY": "$GENE_TEMPLATE" }"""),
                expectedFastaHeaderTemplate = GENE_TEMPLATE,
                expectedFasta = expectedTemplatedAminoAcidFasta,
            ),
            FastaHeaderRequestScenario(
                description = "POST amino acid sequences without explicit header template",
                request = postSample("$ALIGNED_AMINO_ACID_SEQUENCES_ROUTE/$GENE_NAME")
                    .contentType(APPLICATION_JSON)
                    .content("{}"),
                expectedFastaHeaderTemplate = "{primaryKey}",
                expectedFasta = """
                    >1234
                    AAAA
                    >5678
                    GGGG
                    
                """.trimIndent(),
            ),
        )
    }
}

data class FastaHeaderRequestScenario(
    val description: String,
    val request: MockHttpServletRequestBuilder,
    val expectedFastaHeaderTemplate: String,
    val expectedFasta: String,
) {
    override fun toString() = description
}
