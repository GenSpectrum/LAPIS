# LAPIS Service - Development Guide for AI Coding Agents

This guide covers the main LAPIS API service (Kotlin/Spring Boot).

## Build, Test, and Lint Commands

```bash
cd lapis

# Build and test
./gradlew build                    # Full build with tests
./gradlew test                     # Run all tests
./gradlew test --tests "ClassName" # Run specific test class
./gradlew test --tests "ClassName.testMethod" # Run single test method

# Linting and formatting
./gradlew ktlintCheck             # Check code style (CI enforces this)
./gradlew ktlintFormat            # Auto-fix formatting issues

# Running the application
./gradlew bootRun                 # Run locally
./gradlew bootJar                 # Build JAR file

# OpenAPI documentation
./gradlew generateOpenApiDocs     # Generate OpenAPI spec, check the build.gradle for additional options

# ANTLR grammar generation (happens automatically during build)
./gradlew generateGrammarSource
```

## Architecture

LAPIS follows a three-layer architecture:

1. **HTTP Layer** (controllers) - Handles requests/responses, Spring Boot features
   - Located in `org.genspectrum.lapis.controller`
   - Uses Spring annotations (@RestController, @PostMapping, etc.)
   - Handles request validation and response formatting

2. **Query Mapping Layer** - Maps HTTP requests to SILO queries
   - Located in `org.genspectrum.lapis.request`
   - Parses and validates sequence filters, mutations, insertions
   - Maps dynamic metadata fields to query parameters

3. **SILO Client Layer** - Communicates with SILO, handles caching
   - Located in `org.genspectrum.lapis.silo`
   - HTTP client for SILO communication
   - Caffeine cache for performance

### Key Concepts

- **Metadata fields** - Defined in database config YAML
- **Sequence filters** - Derived from metadata fields (equality, regex, ranges)
- **Special properties** - Statically known (limit, orderBy, dataFormat, mutations, etc.)

## Code Style Guidelines

### Formatting

- **Linter:** ktlint 1.4.1 with `ktlint_official` code style
- **Max line length:** 120 characters
- **Trailing commas:** REQUIRED on multi-line call sites and declarations
- **Line endings:** LF with final newline required

### Naming Conventions

- **Packages:** lowercase (e.g., `org.genspectrum.lapis.controller`)
- **Classes:** PascalCase (e.g., `LapisController`, `SequenceFiltersRequest`)
- **Functions:** camelCase (e.g., `getAggregated()`, `handleException()`)
- **Constants:** UPPER_SNAKE_CASE (e.g., `DEFAULT_MIN_PROPORTION`)
- **Test classes:** Suffix with `Test` (e.g., `LapisControllerTest`)
- **Test methods:** Descriptive names, backticks allowed for spaces

### Data Classes

```kotlin
data class SequenceFiltersRequest(
    override val sequenceFilters: SequenceFilters,
    override val nucleotideMutations: List<NucleotideMutation>,
    val fastaHeaderTemplate: String? = null, // trailing comma required
) : CommonSequenceFilters
```

### Error Handling

- Use custom exception hierarchy: `BadRequestException`, `SiloException`, etc.
- Global exception handler with `@ControllerAdvice`
- Return structured error responses with `ProblemDetail`
- Log with appropriate levels (warn for expected errors)

Example:
```kotlin
@ControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequest(ex: BadRequestException): ResponseEntity<ProblemDetail> {
        log.warn("Bad request: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.message))
    }
}
```

## Testing Guidelines

### Framework

- **JUnit 5** - Test framework
- **Spring Boot Test** - Integration testing with Spring context
- **MockK** - Mocking library (NOT Mockito)

### Test Structure

```kotlin
@SpringBootTest
@AutoConfigureMockMvc
class LapisControllerTest(
    @param:Autowired val mockMvc: MockMvc,
) {
    @MockkBean
    lateinit var siloQueryModelMock: SiloQueryModel
    
    @BeforeEach
    fun setup() {
        every { dataVersion.dataVersion } returns "1234"
    }
    
    @Test
    fun `GIVEN some setup WHEN I do something THEN something should happen`() {
        every { siloQueryModelMock.getAggregated(...) } returns Stream.of(...)
        
        mockMvc.perform(post(AGGREGATED_ROUTE).contentType(APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].count").value(0))
    }
    
    @ParameterizedTest
    @MethodSource("{0}")
    fun `GIVEN ... WHEN I do ... THEN something should happen`(testName: String, request: MockHttpServletRequestBuilder) {
        // Test implementation
    }
    
    companion object {
        @JvmStatic
        fun getRequests() = Stream.of(
            Arguments.of("test case 1", get("/endpoint1")),
            Arguments.of("test case 2", post("/endpoint2")),
        )
    }
}
```

### Key Testing Patterns

- **Controller tests** mock the service layer (SiloQueryModel)
- **Use MockMvc** to test full HTTP layer including Spring processing
- **Test both GET and POST endpoints**
- **Test various data formats** (JSON, CSV, TSV, FASTA)
- **Use MockK's `every`** for mocking, not Mockito
- **Parameterized tests** for testing multiple scenarios

### Testing Best Practices

1. Mock at the service layer boundary (SiloQueryModel)
2. Test the full Spring request/response cycle with MockMvc
3. Verify HTTP status codes, headers, and response bodies
4. Test error handling and validation
5. Use descriptive test names with backticks for readability
6. Group related tests in nested classes if needed

## Technology Stack

- **Kotlin:**
- **Spring Boot:**
- **Gradle:**
- **ANTLR:** (for query parsing)
- **Testing:** JUnit 5, MockK, Spring MockMvc
- **OpenAPI:** Springdoc
- **Caching:** Caffeine
- **JSON:** Jackson

## Important Development Notes

### OpenAPI Generation

OpenAPI spec is generated at runtime based on database config. The spec is dynamic because sequence filters depend on the configured metadata fields.
LAPIS usually runs on localhost:8090

- Test manually in Swagger UI at `/swagger-ui/index.html`
- Generate spec with `./gradlew generateOpenApiDocs`
- Use `-Psegmented=true` for multi-segmented genome variant

### ANTLR Grammars

- Located in `src/main/antlr/`
- Used for parsing mutation, insertion, and amino acid filter syntax
- Regenerate with `./gradlew generateGrammarSource`
- Generated code is placed in `build/generated-src/antlr/main/`

### Database Config

- YAML file defines metadata fields
- Determines available sequence filters
- Influences OpenAPI spec generation
- Example location: `src/main/resources/database.yaml`

### Caching

- Uses Caffeine cache for performance
- Caches aggregated, mutation, and insertion queries
- Cache is cleared when SILO data version changes
- Configuration in `org.genspectrum.lapis.config.CacheConfig`

### Multi-segmented Genomes

Project supports both single and multi-segmented genomes:
- Single segment: Standard nucleotide/amino acid mutations
- Multi-segment: Segment-prefixed mutations (e.g., `seg1:A123T`)
- Test both variants when making changes to mutation parsing

### Docker Compose

There is `docker-compose.yml` for local testing.
You can assume that the SILO containers are running.

### CI/CD

- GitHub Actions enforces ktlint on all commits
- Tests must pass before merge
- Multi-platform Docker builds on main branch
- Docker images tagged with git tags and version numbers
