# AGENTS.md - Development Guide for AI Coding Agents

This guide provides essential information for AI coding agents working with the LAPIS codebase.

## Project Overview

LAPIS is a monorepo containing three main components:
- **lapis/** - Main API service (Kotlin/Spring Boot) - [See lapis/AGENTS.md](lapis/AGENTS.md)
- **lapis-docs/** - Documentation website (TypeScript/Astro) - [See lapis-docs/AGENTS.md](lapis-docs/AGENTS.md)
- **lapis-e2e/** - End-to-end integration tests (TypeScript/Mocha) - [See lapis-e2e/AGENTS.md](lapis-e2e/AGENTS.md)

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
docs(lapis-docs): update API reference
```

**Breaking Changes:** Use `BREAKING CHANGE:` in commit body

**Enforcement:** PR titles are validated via commitlint in CI

## Technology Stack

- **Kotlin:**
- **Spring Boot:**
- **Gradle:**
- **ANTLR:** (for query parsing)
- **Testing:** JUnit 5, MockK, Spring MockMvc, Mocha, Chai
- **OpenAPI:** Springdoc
- **Caching:** Caffeine
- **Node.js Projects:** npm for lapis-e2e and lapis-docs
- **Documentation:** Astro (static site generator)

## Important Development Notes

1. **OpenAPI Generation:** OpenAPI spec is generated at runtime based on database config. Test manually in Swagger UI.

2. **ANTLR Grammars:** Located in `lapis/src/main/antlr/`. Regenerate with `./gradlew generateGrammarSource`.

3. **Database Config:** YAML file defines metadata fields, determines available sequence filters.

4. **Caching:** Caffeine cache for aggregated/mutation/insertion queries. Cleared on SILO data version change.

5. **Multi-segmented Genomes:** Project supports both single and multi-segmented genomes. Test both variants.

6. **Docker Compose:** Use `docker-compose.yml` in `lapis/` for local development with SILO.

7. **CI/CD:** GitHub Actions enforces ktlint, prettier, tests. Multi-platform Docker builds on main branch.

8. **Release Process:** Automated via Release Please. Merging release PR creates Git tags and Docker image tags.
