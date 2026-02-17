# AGENTS.md - Development Guide for AI Coding Agents

This guide provides essential information for AI coding agents working with the LAPIS codebase.

## Project Overview

LAPIS is a monorepo containing three main components:
- **lapis/** - Main API service (Kotlin/Spring Boot)
- **lapis-docs/** - Documentation website (TypeScript/Astro)
- **lapis-e2e/** - End-to-end integration tests (TypeScript/Mocha)

## Build, Test, and Lint Commands

### Kotlin Service (lapis/)

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
./gradlew generateOpenApiDocs     # Generate OpenAPI spec
./gradlew generateOpenApiDocs -Psegmented=true  # For multi-segmented genomes

# ANTLR grammar generation (happens automatically during build)
./gradlew generateGrammarSource
```

### E2E Tests (lapis-e2e/)

```bash
cd lapis-e2e

# Setup
npm install
npm run generate-client           # Generate TypeScript client from OpenAPI spec

# Testing
npm test                          # Run all E2E tests
npm test -- --grep "aggregated"   # Run tests matching pattern

# Formatting
npm run check-format              # Check Prettier formatting (CI enforces this)
npm run format                    # Auto-fix formatting
```

### Documentation Site (lapis-docs/)

```bash
cd lapis-docs

npm install
npm run dev                       # Start dev server
npm run build                     # Build for production
npm run check-format              # Check formatting
npm run format                    # Auto-fix formatting
npm run check-types               # TypeScript type checking
```

## Architecture

LAPIS follows a three-layer architecture:

1. **HTTP Layer** (controllers) - Handles requests/responses, Spring Boot features
2. **Query Mapping Layer** - Maps HTTP requests to SILO queries
3. **SILO Client Layer** - Communicates with SILO, handles caching

Key concepts:
- **Metadata fields** - Defined in database config YAML
- **Sequence filters** - Derived from metadata fields (equality, regex, ranges)
- **Special properties** - Statically known (limit, orderBy, dataFormat, mutations, etc.)

## Code Style Guidelines

### Kotlin (lapis/)

**Formatting:**
- Linter: ktlint 1.4.1 with `ktlint_official` code style
- Max line length: 120 characters
- Trailing commas REQUIRED on multi-line call sites and declarations
- LF line endings, final newline required

**Import Organization:**
```kotlin
// 1. Standard library
import kotlin.collections.*

// 2. Third-party libraries
import com.fasterxml.jackson.databind.JsonNode

// 3. Spring framework
import org.springframework.web.bind.annotation.PostMapping

// 4. Project imports
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.request.SequenceFiltersRequest
```

**Naming Conventions:**
- Packages: lowercase (e.g., `org.genspectrum.lapis.controller`)
- Classes: PascalCase (e.g., `LapisController`, `SequenceFiltersRequest`)
- Functions: camelCase (e.g., `getAggregated()`, `handleException()`)
- Constants: UPPER_SNAKE_CASE (e.g., `DEFAULT_MIN_PROPORTION`)
- Test classes: Suffix with `Test` (e.g., `LapisControllerTest`)
- Test methods: Descriptive names, backticks allowed for spaces

**Data Classes:**
```kotlin
data class SequenceFiltersRequest(
    override val sequenceFilters: SequenceFilters,
    override val nucleotideMutations: List<NucleotideMutation>,
    val fastaHeaderTemplate: String? = null, // trailing comma required
) : CommonSequenceFilters
```

**Error Handling:**
- Use custom exception hierarchy: `BadRequestException`, `SiloException`, etc.
- Global exception handler with `@ControllerAdvice`
- Return structured error responses with `ProblemDetail`
- Log with appropriate levels (warn for expected errors)

### TypeScript (lapis-e2e/)

**Prettier Configuration:**
```json
{
  "printWidth": 110,
  "trailingComma": "es5",
  "semi": true,
  "singleQuote": true,
  "arrowParens": "avoid"
}
```

**Code Patterns:**
- Arrow functions preferred
- `const` for variables
- Interface definitions for types
- Async/await for promises
- Destructuring common

### TypeScript (lapis-docs/)

**Prettier Configuration:**
```json
{
  "printWidth": 120,
  "tabWidth": 4,
  "trailingComma": "all",
  "semi": true,
  "singleQuote": true
}
```

- Strict TypeScript mode enabled

## Testing Guidelines

### Unit Tests (Kotlin)

**Framework:** JUnit 5 + Spring Boot Test + MockK

**Test Structure:**
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
    fun `test aggregated endpoint returns data`() {
        every { siloQueryModelMock.getAggregated(...) } returns Stream.of(...)
        
        mockMvc.perform(post(AGGREGATED_ROUTE).contentType(APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].count").value(0))
    }
    
    @ParameterizedTest
    @MethodSource("getRequests")
    fun parametrizedTest(testName: String, request: MockHttpServletRequestBuilder) {
        // Test implementation
    }
}
```

**Key Patterns:**
- Controller tests mock the service layer (SiloQueryModel)
- Use `MockMvc` to test full HTTP layer including Spring processing
- Test both GET and POST endpoints
- Test various data formats (JSON, CSV, TSV, FASTA)
- Use MockK's `every` for mocking, not Mockito

### E2E Tests (TypeScript)

**Framework:** Mocha + Chai

**Test Structure:**
```typescript
describe('The /aggregated endpoint', () => {
  it('should return data for the test case', async () => {
    const result = await lapisClient.postAggregated({
      aggregatedPostRequest: testCase.lapisRequest,
    });
    expect(result.data).to.have.deep.members(testCase.expected);
  });
});
```

**Purpose:**
- Test LAPIS and SILO working together
- Validate OpenAPI spec via generated client
- Use generated client when possible, plain fetch for edge cases

## Commit Message Convention

**Format:** `<type>(<scope>): <description>`

**Required Scopes:**
- `lapis` - Main Kotlin service
- `lapis-docs` - Documentation site
- `lapis-e2e` - E2E tests
- `github-actions` - CI/CD workflows
- `root` - Root-level changes

**Types:** `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `ci`

**Examples:**
```
feat(lapis): add new endpoint /queriesOverTime
fix(lapis-e2e): fix invalid insertion indices
chore(lapis): bump gradle-wrapper from 8.14.3 to 9.3.1
```

**Breaking Changes:** Use `BREAKING CHANGE:` in commit body

**Enforcement:** PR titles are validated via commitlint in CI

## Technology Stack

- **Kotlin:** 2.3.0, JVM target 21
- **Spring Boot:** 3.5.6
- **Gradle:** 9.3.1
- **ANTLR:** 4.13.2 (for query parsing)
- **Testing:** JUnit 5, MockK, Spring MockMvc
- **OpenAPI:** Springdoc 2.8.13
- **Caching:** Caffeine
- **Node.js Projects:** npm for lapis-e2e and lapis-docs

## Important Development Notes

1. **OpenAPI Generation:** OpenAPI spec is generated at runtime based on database config. Test manually in Swagger UI.

2. **ANTLR Grammars:** Located in `lapis/src/main/antlr/`. Regenerate with `./gradlew generateGrammarSource`.

3. **Database Config:** YAML file defines metadata fields, determines available sequence filters.

4. **Caching:** Caffeine cache for aggregated/mutation/insertion queries. Cleared on SILO data version change.

5. **Multi-segmented Genomes:** Project supports both single and multi-segmented genomes. Test both variants.

6. **Docker Compose:** Use `docker-compose.yml` in `lapis/` for local development with SILO.

7. **CI/CD:** GitHub Actions enforces ktlint, prettier, tests. Multi-platform Docker builds on main branch.

8. **Release Process:** Automated via Release Please. Merging release PR creates Git tags and Docker image tags.
