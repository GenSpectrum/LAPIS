# Implementation Plan: Advanced Query Metadata Collector

## Overview
Create a new ANTLR listener that collects metadata fields and mutation positions from advanced queries, ignoring anything inside MAYBE expressions. This will mirror the structure of the existing `AdvancedQueryCustomListener` but focus on information extraction rather than query building.

## Requirements Summary
- Collect all metadata field names used in queries
- Collect nucleotide mutation positions as (segment, position) pairs
- Collect amino acid mutation positions as (gene, position) pairs
- Skip anything inside MAYBE expressions
- Only collect mutations, NOT insertions

## Implementation Steps

### 1. Create Data Classes (`QueryMetadata.kt`)

**File**: `src/main/kotlin/org/genspectrum/lapis/model/QueryMetadata.kt`

Create data classes to hold collected information:

```kotlin
package org.genspectrum.lapis.model

data class QueryMetadata(
    val metadataFields: Set<String>,
    val nucleotideMutations: Set<NucleotideMutationPosition>,
    val aminoAcidMutations: Set<AminoAcidMutationPosition>
)

data class NucleotideMutationPosition(
    val segment: String?,  // null for single-segmented genomes
    val position: Int
)

data class AminoAcidMutationPosition(
    val gene: String,
    val position: Int
)
```

**Key decisions**:
- Use `Set` for automatic deduplication
- Nullable `segment` matches existing `NucleotideSymbolEquals` pattern
- Non-nullable `gene` matches existing `AminoAcidSymbolEquals` pattern

### 2. Create Collector Listener (`AdvancedQueryMetadataCollector.kt`)

**File**: `src/main/kotlin/org/genspectrum/lapis/model/AdvancedQueryMetadataCollector.kt`

#### Core Structure

```kotlin
package org.genspectrum.lapis.model

import AdvancedQueryBaseListener
import AdvancedQueryParser.*
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.antlr.v4.runtime.tree.ParseTreeListener
import java.util.Locale

class AdvancedQueryMetadataCollector(
    private val referenceGenomeSchema: ReferenceGenomeSchema,
    databaseConfig: DatabaseConfig,
) : AdvancedQueryBaseListener(), ParseTreeListener {

    private val metadataFieldsByName = databaseConfig.schema.metadata
        .associateBy { it.name.lowercase(Locale.US) }

    private val metadataFields = mutableSetOf<String>()
    private val nucleotideMutations = mutableSetOf<NucleotideMutationPosition>()
    private val aminoAcidMutations = mutableSetOf<AminoAcidMutationPosition>()

    private var maybeDepth = 0

    fun getCollectedMetadata(): QueryMetadata {
        return QueryMetadata(
            metadataFields = metadataFields.toSet(),
            nucleotideMutations = nucleotideMutations.toSet(),
            aminoAcidMutations = aminoAcidMutations.toSet()
        )
    }

    private fun shouldSkip(): Boolean = maybeDepth > 0
}
```

#### MAYBE Tracking Methods

```kotlin
override fun enterMaybe(ctx: MaybeContext?) {
    maybeDepth++
}

override fun enterVariantMaybe(ctx: VariantMaybeContext?) {
    maybeDepth++
}

override fun exitMaybe(ctx: MaybeContext?) {
    maybeDepth--
}

override fun exitVariantMaybe(ctx: VariantMaybeContext?) {
    maybeDepth--
}
```

**Rationale**: Counter-based approach handles nested MAYBE expressions correctly.

#### Metadata Collection Methods

```kotlin
override fun enterMetadataQuery(ctx: MetadataQueryContext) {
    if (shouldSkip()) return

    val metadataName = ctx.name().text
    val fieldName = when {
        metadataName.endsWith(".regex", ignoreCase = true) ->
            metadataName.substringBeforeLast(".")
        metadataName.endsWith(".PhyloDescendantOf", ignoreCase = true) ->
            metadataName.substringBeforeLast(".")
        else -> metadataName
    }

    metadataFieldsByName[fieldName.lowercase(Locale.US)]?.let {
        metadataFields.add(it.name)
    }
}

override fun enterMetadataGreaterThanEqualQuery(ctx: MetadataGreaterThanEqualQueryContext) {
    if (shouldSkip()) return
    val metadataName = ctx.name()[0].text
    metadataFieldsByName[metadataName.lowercase(Locale.US)]?.let {
        metadataFields.add(it.name)
    }
}

override fun enterMetadataLessThanEqualQuery(ctx: MetadataLessThanEqualQueryContext) {
    if (shouldSkip()) return
    val metadataName = ctx.name()[0].text
    metadataFieldsByName[metadataName.lowercase(Locale.US)]?.let {
        metadataFields.add(it.name)
    }
}

override fun enterIsNullQuery(ctx: IsNullQueryContext) {
    if (shouldSkip()) return
    val metadataName = ctx.name().text
    metadataFieldsByName[metadataName.lowercase(Locale.US)]?.let {
        metadataFields.add(it.name)
    }
}
```

**Key points**:
- All methods check `shouldSkip()` first
- Handle special suffixes (`.regex`, `.PhyloDescendantOf`)
- Case-insensitive lookup, store canonical name from schema
- Use safe navigation (`?.let`) to skip invalid field names

#### Mutation Collection Methods

```kotlin
override fun enterSingleSegmentedMutationQuery(ctx: SingleSegmentedMutationQueryContext) {
    if (shouldSkip()) return
    val position = ctx.position().text.toInt()
    nucleotideMutations.add(NucleotideMutationPosition(null, position))
}

override fun enterNamedMutationQuery(ctx: NamedMutationQueryContext) {
    if (shouldSkip()) return

    val position = ctx.position().text.toInt()
    val name = ctx.name().text

    // Try gene first (amino acid mutation)
    when (val gene = referenceGenomeSchema.getGene(name)?.name) {
        is String -> {
            aminoAcidMutations.add(AminoAcidMutationPosition(gene, position))
            return
        }
    }

    // Try nucleotide sequence (nucleotide mutation)
    when (val segmentName = referenceGenomeSchema.getNucleotideSequence(name)?.name) {
        is String -> {
            nucleotideMutations.add(NucleotideMutationPosition(segmentName, position))
            return
        }
    }

    // If neither matches, silently skip (this is a collector, not a validator)
}
```

**Key points**:
- Mirror the logic from `AdvancedQueryCustomListener.enterNamedMutationQuery()`
- Try gene first, then segment (same precedence as existing code)
- Silently skip invalid names (permissive approach for collection)
- DO NOT override insertion methods (`enterNucleotideInsertionQuery`, `enterNamedInsertionQuery`)

### 3. Integrate into Facade (`AdvancedQueryFacade.kt`)

**File**: `src/main/kotlin/org/genspectrum/lapis/model/AdvancedQueryFacade.kt`

Add new method following the pattern of the existing `map()` method:

```kotlin
fun collectMetadata(advancedQuery: String): QueryMetadata {
    val lexer = AdvancedQueryLexer(CharStreams.fromString(advancedQuery))
    val tokens = CommonTokenStream(lexer)
    val parser = AdvancedQueryParser(tokens)
    parser.removeErrorListeners()
    parser.addErrorListener(ThrowingAdvancedQueryErrorListener())

    val collector = AdvancedQueryMetadataCollector(referenceGenomeSchema, databaseConfig)
    val walker = ParseTreeWalker()
    walker.walk(collector, parser.start())

    return collector.getCollectedMetadata()
}
```

**Rationale**: Exact same structure as `map()` for consistency.

### 4. Create Comprehensive Tests

**File**: `src/test/kotlin/org/genspectrum/lapis/model/AdvancedQueryMetadataCollectorTest.kt`

#### Test Structure

```kotlin
package org.genspectrum.lapis.model

import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.config.ReferenceSequenceSchema
import org.genspectrum.lapis.dummyDatabaseConfig
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test

class AdvancedQueryMetadataCollectorTest {
    private val referenceGenomeSchema = ReferenceGenomeSchema(
        listOf(ReferenceSequenceSchema("main")),
        listOf(ReferenceSequenceSchema("S"), ReferenceSequenceSchema("ORF1a"))
    )
    private val underTest = AdvancedQueryFacade(referenceGenomeSchema, dummyDatabaseConfig)
}
```

#### Test Categories

**Basic Metadata Collection**:
- Simple equals queries
- Range queries (>=, <=)
- Regex queries (.regex suffix)
- Null checks (IsNull)
- PhyloDescendantOf queries

**Mutation Collection**:
- Single-segmented nucleotide mutations (e.g., "300G")
- Named amino acid mutations (e.g., "S:501Y")
- Named nucleotide mutations in multi-segment (e.g., "main:300G")
- Mutations without symbols (e.g., "300", "S:501")

**MAYBE Skipping**:
- Skip metadata inside MAYBE
- Skip mutations inside MAYBE
- Handle nested MAYBE expressions
- Complex queries with both MAYBE and non-MAYBE parts

**Insertion Skipping**:
- Verify nucleotide insertions are NOT collected
- Verify amino acid insertions are NOT collected

**Complex Expressions**:
- N-of expressions
- Nested boolean expressions (AND, OR, NOT)
- Deduplication (same position multiple times)

**Example Test**:
```kotlin
@Test
fun `should collect metadata and mutations, skipping MAYBE`() {
    val query = "country=Switzerland & 300G & MAYBE(400T | region=Zurich) & S:501Y"

    val result = underTest.collectMetadata(query)

    assertThat(result.metadataFields, containsInAnyOrder("country"))
    assertThat(result.nucleotideMutations,
        containsInAnyOrder(NucleotideMutationPosition(null, 300)))
    assertThat(result.aminoAcidMutations,
        containsInAnyOrder(AminoAcidMutationPosition("S", 501)))
}
```

### 5. Multi-Segment Tests

**File**: `src/test/kotlin/org/genspectrum/lapis/model/AdvancedQueryMetadataCollectorMultiSegmentTest.kt`

Create similar test suite with multi-segmented genome setup:

```kotlin
private val referenceGenomeSchema = ReferenceGenomeSchema(
    listOf(
        ReferenceSequenceSchema("segment1"),
        ReferenceSequenceSchema("segment2")
    ),
    listOf(ReferenceSequenceSchema("S"), ReferenceSequenceSchema("ORF1a"))
)
```

Test that segment names are correctly captured in multi-segmented queries.

## Critical Files

### Files to Create:
1. `src/main/kotlin/org/genspectrum/lapis/model/QueryMetadata.kt` (~25 lines)
2. `src/main/kotlin/org/genspectrum/lapis/model/AdvancedQueryMetadataCollector.kt` (~200 lines)
3. `src/test/kotlin/org/genspectrum/lapis/model/AdvancedQueryMetadataCollectorTest.kt` (~500 lines)
4. `src/test/kotlin/org/genspectrum/lapis/model/AdvancedQueryMetadataCollectorMultiSegmentTest.kt` (~200 lines)

### Files to Modify:
1. `src/main/kotlin/org/genspectrum/lapis/model/AdvancedQueryFacade.kt` (add `collectMetadata()` method, ~15 lines)

### Reference Files (for patterns):
- `src/main/kotlin/org/genspectrum/lapis/model/AdvancedQueryCustomListener.kt` - Gene/segment disambiguation logic
- `src/main/kotlin/org/genspectrum/lapis/config/ReferenceGenome.kt` - ReferenceGenomeSchema API
- `src/test/kotlin/org/genspectrum/lapis/model/AdvancedQueryFacadeTest.kt` - Test setup patterns
- `src/main/antlr/org/genspectrum/lapis/model/advancedqueryparser/AdvancedQuery.g4` - Grammar rules

## Implementation Sequence

1. Create `QueryMetadata.kt` with data classes
2. Create skeleton of `AdvancedQueryMetadataCollector.kt` with MAYBE tracking
3. Implement metadata collection methods
4. Implement mutation collection methods
5. Add `collectMetadata()` method to `AdvancedQueryFacade`
6. Write and run basic tests
7. Write and run MAYBE skipping tests
8. Write and run complex expression tests
9. Create multi-segment test suite

## Key Design Decisions

1. **Counter-based MAYBE tracking**: Handles nested MAYBE expressions correctly
2. **Permissive collection**: Silently skip invalid field/gene/segment names (collector role, not validator)
3. **Case-insensitive lookups**: Store canonical names from schema
4. **Automatic deduplication**: Using `Set` deduplicates same positions with different symbols
5. **No insertion collection**: Achieved by not overriding insertion enter methods
6. **Gene precedence**: Try gene before segment (matches existing behavior)

## Expected Behavior Examples

```kotlin
// Example 1: Basic collection
"country=USA & 300G & S:501Y"
→ metadataFields: ["country"]
→ nucleotideMutations: [(null, 300)]
→ aminoAcidMutations: [("S", 501)]

// Example 2: MAYBE skipping
"300G & MAYBE(400T | country=USA)"
→ metadataFields: []
→ nucleotideMutations: [(null, 300)]
→ aminoAcidMutations: []

// Example 3: Deduplication
"300G & 300T & 300-"
→ nucleotideMutations: [(null, 300)]  // Single entry

// Example 4: Multi-segment
"seg1:100A & S:200K"
→ nucleotideMutations: [("seg1", 100)]
→ aminoAcidMutations: [("S", 200)]
```
