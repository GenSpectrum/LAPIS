# Implementation Plan: Random with Seed Support

## Overview

Add support for `orderBy=random(<seed>)` syntax in LAPIS API to allow deterministic randomization of query results.

### Current State
- `orderBy=random` works (randomizes results)
- Backend `RandomizeConfig.WithSeed(seed: Int)` exists from commit c663e2df
- Caching logic updated to exclude randomized queries

### Goal
Support both:
- **GET**: `?orderBy=random` or `?orderBy=random(123)`
- **POST**: `{"orderBy": [{"field": "country"}]}` OR `{"orderBy": {"random": true}}` OR `{"orderBy": {"random": 123}}`

### Constraints
- Random ordering and field ordering are **mutually exclusive**
- `?orderBy=country,random` should be rejected

---

## API Design

### GET Requests (Query Parameters)

**Valid:**
- `?orderBy=country,date` → normal field ordering
- `?orderBy=random` → randomize without seed
- `?orderBy=random(42)` → randomize with seed

**Invalid:**
- `?orderBy=country,random` → ❌ REJECT (mixed)
- `?orderBy=random,date` → ❌ REJECT (mixed)

### POST Requests (JSON Body)

**Field Ordering:**
```json
{
  "orderBy": [
    {"field": "country", "type": "ascending"},
    {"field": "date"}
  ]
}
```

**Random Ordering:**
```json
{
  "orderBy": {"random": true}
}
```

**Random with Seed:**
```json
{
  "orderBy": {"random": 123}
}
```

---

## Architecture Changes

### New Data Structures

#### OrderBySpec (Sealed Class)
```kotlin
sealed class OrderBySpec {
    data class ByFields(val fields: List<OrderByField>) : OrderBySpec()
    data class Random(val seed: Int?) : OrderBySpec()
}
```

#### OrderByField (Unchanged)
```kotlin
data class OrderByField(
    val field: String,
    val order: Order,
)
// No changes needed - stays simple!
```

### Flow Changes

#### Current Flow (GET)
1. `?orderBy=random` → `List<OrderByField>` with field="random"
2. SiloAction factory extracts "random" → `randomize=true`
3. Removes "random" from orderByFields list

#### New Flow (GET)
1. `?orderBy=random(123)` → `List<OrderByField>` with field="random(123)" (stored as string)
2. Validation: if contains field starting with "random", must be only element
3. Convert to `OrderBySpec.Random(seed=123)` by parsing "random(123)" → extract seed
4. SiloAction factory: `OrderBySpec.Random(123)` → `RandomizeConfig.WithSeed(123)`

#### New Flow (POST)
1. JSON: `{"orderBy": {"random": 123}}`
2. `OrderBySpecDeserializer` parses → `OrderBySpec.Random(seed=123)`
3. SiloAction factory: `OrderBySpec.Random(123)` → `RandomizeConfig.WithSeed(123)`

---

## Files to Modify

### Core Files

| File | Purpose | Changes |
|------|---------|---------|
| `OrderByField.kt` | Request parsing | Add seed field, OrderBySpec, deserializers |
| `SpecialProperties.kt` | JSON schema | Allow OBJECT type for orderBy |
| `CommonSequenceFilters.kt` | Request parsing | Use OrderBySpec instead of List |
| `SiloQuery.kt` | Query building | Update factories to accept OrderBySpec |
| `SequenceFiltersRequest*.kt` | Request objects | Migrate to OrderBySpec |
| `LapisController.kt` | API endpoints | Add validation for GET |
| `OrderByValidator.kt` | Validation | (New file) Validation logic |

### Test Files
- `SequenceFiltersRequestWithFieldsTest.kt`
- `SiloQueryTest.kt`
- New tests for OrderBySpec parsing

---

## Implementation Stages

### Stage 1: Add Infrastructure (Backwards Compatible)
**Commit:** `feat(lapis): add OrderBySpec sealed class`

**Files:**
- `src/main/kotlin/org/genspectrum/lapis/request/OrderByField.kt`

**Changes:**
1. Create `OrderBySpec` sealed class with `ByFields` and `Random` variants (add to bottom of file)
2. Add helper extension functions to convert between types:
   ```kotlin
   sealed class OrderBySpec {
       data class ByFields(val fields: List<OrderByField>) : OrderBySpec()
       data class Random(val seed: Int?) : OrderBySpec()
   }

   fun List<OrderByField>.toOrderBySpec(): OrderBySpec {
       val randomField = find { it.field.startsWith("random") }

       return when {
           randomField == null -> OrderBySpec.ByFields(this)
           randomField.field == "random" -> OrderBySpec.Random(seed = null)
           else -> {
               // Parse "random(123)" to extract seed
               val seedPattern = Regex("^random\\((\\d+)\\)$")
               val match = seedPattern.matchEntire(randomField.field)
               val seed = match?.groupValues?.get(1)?.toInt()
               OrderBySpec.Random(seed = seed)
           }
       }
   }

   fun OrderBySpec.toOrderByFields(): List<OrderByField> = when (this) {
       is OrderBySpec.ByFields -> fields
       is OrderBySpec.Random -> emptyList()
   }
   ```

**Status:** ✅ Compiles - Backwards compatible, OrderByField unchanged

---

### Stage 2: Add GET Parsing for random(seed)
**Commit:** `feat(lapis): support random(seed) syntax in query parameters`

**Files:**
- `src/main/kotlin/org/genspectrum/lapis/request/OrderByField.kt`

**Changes:**
1. Update `OrderByFieldConverter.convert()` to validate and preserve "random(...)" fields:
   ```kotlin
   @Component
   class OrderByFieldConverter(
       private val orderByFieldsCleaner: OrderByFieldsCleaner,
   ) : Converter<String, OrderByField> {
       override fun convert(source: String): OrderByField {
           val field = if (source.startsWith("random")) {
               // Validate format: must be "random" or "random(<digits>)"
               val validRandomPattern = Regex("^random(\\(\\d+\\))?$")
               if (!validRandomPattern.matches(source)) {
                   throw BadRequestException(
                       "Invalid random orderBy format: '$source'. " +
                       "Use 'random' or 'random(<seed>)' where seed is a positive integer."
                   )
               }
               source  // Keep as-is: "random" or "random(123)"
           } else {
               orderByFieldsCleaner.clean(source)
           }

           return OrderByField(field = field, order = Order.ASCENDING)
       }
   }
   ```

**Status:** ✅ Compiles - Validates format and stores "random(123)" as field name, parsed later in toOrderBySpec()

---

### Stage 3: Add POST JSON Parsing for OrderBySpec
**Commit:** `feat(lapis): add OrderBySpec deserializer for array/object formats`

**Files:**
- `src/main/kotlin/org/genspectrum/lapis/request/OrderByField.kt`

**Changes:**

1. Add new `OrderBySpecDeserializer` class:
   ```kotlin
   @JsonComponent
   class OrderBySpecDeserializer(
       private val orderByFieldsCleaner: OrderByFieldsCleaner,
   ) : JsonDeserializer<OrderBySpec>() {
       override fun deserialize(p: JsonParser, ctxt: DeserializationContext): OrderBySpec {
           val node = p.readValueAsTree<JsonNode>()

           return when {
               node.isArray -> {
                   val fields = node.map { fieldNode ->
                       deserializeOrderByField(fieldNode, p.codec)
                   }
                   OrderBySpec.ByFields(fields)
               }
               node.isObject && node.has("random") -> {
                   val randomValue = node.get("random")
                   val seed = when {
                       randomValue.isBoolean && randomValue.asBoolean() -> null
                       randomValue.isBoolean && !randomValue.asBoolean() ->
                           throw BadRequestException("random must be true or an integer seed")
                       randomValue.isInt -> randomValue.asInt()
                       else -> throw BadRequestException("random must be true or an integer seed")
                   }
                   OrderBySpec.Random(seed)
               }
               else -> throw BadRequestException(
                   "orderBy must be an array of fields or {random: true|<seed>}"
               )
           }
       }

       private fun deserializeOrderByField(node: JsonNode, codec: ObjectCodec): OrderByField {
           // Use existing OrderByFieldDeserializer logic
           return codec.treeToValue(node, OrderByField::class.java)
       }
   }
   ```

**Status:** ✅ Compiles - New deserializer exists but not yet used

---

### Stage 4: Migrate to OrderBySpec Throughout Codebase
**Commit:** `feat(lapis): migrate all code to use OrderBySpec`

**Files:**
- `src/main/kotlin/org/genspectrum/lapis/request/CommonSequenceFilters.kt`
- `src/main/kotlin/org/genspectrum/lapis/silo/SiloQuery.kt`
- `src/main/kotlin/org/genspectrum/lapis/request/SequenceFiltersRequest.kt`
- `src/main/kotlin/org/genspectrum/lapis/request/SequenceFiltersRequestWithFields.kt`
- Other request classes implementing `CommonSequenceFilters`

**Changes:**

1. **CommonSequenceFilters.kt** - Update interface and parsing:
   ```kotlin
   interface CommonSequenceFilters : BaseSequenceFilters {
       val orderByFields: OrderBySpec  // Type changed from List<OrderByField>
       val limit: Int?
       val offset: Int?
   }

   fun parseCommonFields(
       node: JsonNode,
       codec: ObjectCodec,
   ): ParsedCommonFields {
       val parsedMutationsAndInsertions = parseMutationsAndInsertions(node, codec)

       val orderByFields = when (val orderByNode = node.get(ORDER_BY_PROPERTY)) {
           null -> OrderBySpec.ByFields(emptyList())
           else -> codec.treeToValue(orderByNode, OrderBySpec::class.java)
       }

       // ... rest of parsing

       return ParsedCommonFields(
           nucleotideMutations = parsedMutationsAndInsertions.nucleotideMutations,
           aminoAcidMutations = parsedMutationsAndInsertions.aminoAcidMutations,
           nucleotideInsertions = parsedMutationsAndInsertions.nucleotideInsertions,
           aminoAcidInsertions = parsedMutationsAndInsertions.aminoAcidInsertions,
           sequenceFilters = sequenceFilters,
           orderByFields = orderByFields,
           limit = limit,
           offset = offset,
       )
   }

   data class ParsedCommonFields(
       val nucleotideMutations: List<NucleotideMutation>,
       val aminoAcidMutations: List<AminoAcidMutation>,
       val nucleotideInsertions: List<NucleotideInsertion>,
       val aminoAcidInsertions: List<AminoAcidInsertion>,
       val sequenceFilters: SequenceFilters,
       val orderByFields: OrderBySpec,  // Type changed from List<OrderByField>
       val limit: Int?,
       val offset: Int?,
   )
   ```

2. **SiloQuery.kt** - Update factory methods:
   ```kotlin
   // Add helper functions
   private fun getRandomize(orderByFields: OrderBySpec): RandomizeConfig = when (orderByFields) {
       is OrderBySpec.ByFields -> RandomizeConfig.Disabled
       is OrderBySpec.Random -> orderByFields.seed?.let { RandomizeConfig.WithSeed(it) }
           ?: RandomizeConfig.Enabled
   }

   private fun getOrderByFieldsList(orderByFields: OrderBySpec): List<OrderByField> = when (orderByFields) {
       is OrderBySpec.ByFields -> orderByFields.fields
       is OrderBySpec.Random -> emptyList()
   }

   // Update all factory methods to accept OrderBySpec
   fun aggregated(
       groupByFields: List<String> = emptyList(),
       orderByFields: OrderBySpec = OrderBySpec.ByFields(emptyList()),
       limit: Int? = null,
       offset: Int? = null,
   ): SiloAction<AggregationData> =
       AggregatedAction(
           groupByFields = groupByFields,
           orderByFields = getOrderByFieldsList(orderByFields),
           randomize = getRandomize(orderByFields),
           limit = limit,
           offset = offset,
       )

   // Repeat for all factory methods: mutations, aminoAcidMutations, details,
   // nucleotideInsertions, aminoAcidInsertions, genomicSequence

   // Remove old helper functions
   // Delete: old getRandomize(List<OrderByField>)
   // Delete: getNonRandomizedOrderByFields()
   ```

3. **Request Classes** - Update all implementations:
   - Change `orderByFields` type from `List<OrderByField>` to `OrderBySpec` in all classes implementing `CommonSequenceFilters`
   - Update constructors to accept `OrderBySpec`
   - Update all call sites where these classes are instantiated to pass `OrderBySpec`

**Status:** ✅ Compiles - Complete migration, field name unchanged for compatibility

---

### Stage 5: Add GET Validation
**Commit:** `feat(lapis): add validation to reject mixed random and field ordering`

**Files:**
- `src/main/kotlin/org/genspectrum/lapis/request/OrderByValidator.kt` (NEW)
- Controller files where GET parameters are processed

**Changes:**

1. **OrderByValidator.kt** (new file):
   ```kotlin
   package org.genspectrum.lapis.request

   import org.genspectrum.lapis.controller.BadRequestException
   import org.genspectrum.lapis.silo.ORDER_BY_RANDOM_FIELD_NAME

   object OrderByValidator {
       fun validateAndConvert(orderByFields: List<OrderByField>?): OrderBySpec {
           if (orderByFields == null || orderByFields.isEmpty()) {
               return OrderBySpec.ByFields(emptyList())
           }

           val hasRandom = orderByFields.any { it.field == ORDER_BY_RANDOM_FIELD_NAME }

           if (hasRandom && orderByFields.size > 1) {
               throw BadRequestException(
                   "Cannot mix 'random' with other orderBy fields. " +
                   "Use either 'orderBy=random' or 'orderBy=field1,field2'"
               )
           }

           return orderByFields.toOrderBySpec()
       }
   }
   ```

2. Update controller or request parsing to use validator before creating requests

**Status:** ✅ Compiles - Validation added

---

### Stage 6: Add Tests
**Commit:** `test(lapis): add tests for random with seed functionality`

**Files:**
- `src/test/kotlin/org/genspectrum/lapis/request/OrderByFieldTest.kt` (NEW)
- `src/test/kotlin/org/genspectrum/lapis/request/SequenceFiltersRequestWithFieldsTest.kt`
- `src/test/kotlin/org/genspectrum/lapis/silo/SiloQueryTest.kt`

**Test Cases:**

1. **OrderByFieldConverter Tests:**
   - `random` → OrderByField(field="random")
   - `random(123)` → OrderByField(field="random(123)")
   - `random(0)` → OrderByField(field="random(0)")
   - `country` → OrderByField(field="country")
   - `random(abc)` → BadRequestException (invalid format)
   - `random()` → BadRequestException (missing seed)
   - `randomX` → BadRequestException (invalid format)
   - `random(12.3)` → BadRequestException (non-integer seed)

2. **OrderBySpecDeserializer Tests:**
   - `[{"field": "country"}]` → ByFields
   - `{"random": true}` → Random(null)
   - `{"random": 123}` → Random(123)
   - Invalid formats throw BadRequestException

3. **List<OrderByField>.toOrderBySpec() Tests:**
   - `[OrderByField("country"), OrderByField("date")]` → ByFields([country, date])
   - `[OrderByField("random")]` → Random(null)
   - `[OrderByField("random(123)")]` → Random(123)

4. **OrderByValidator Tests:**
   - `[OrderByField("country"), OrderByField("date")]` → ByFields([country, date])
   - `[OrderByField("random")]` → Random(null)
   - `[OrderByField("random(123)")]` → Random(123)
   - `[OrderByField("country"), OrderByField("random")]` → BadRequestException

5. **SiloQuery Tests:**
   - OrderBySpec.Random(null) → RandomizeConfig.Enabled
   - OrderBySpec.Random(123) → RandomizeConfig.WithSeed(123)
   - OrderBySpec.ByFields([...]) → RandomizeConfig.Disabled

6. **Integration Tests:**
   - GET `?orderBy=random(42)` → correct SiloQuery
   - POST `{"orderBy": {"random": 42}}` → correct SiloQuery
   - Caching behaves correctly (no cache for randomized)

**Status:** ✅ Compiles - Tests added

---

## Summary Table

| Stage | Commit Message | Files | Breaking? | Compiles? |
|-------|---------------|-------|-----------|-----------|
| 1 | `feat(lapis): add OrderBySpec sealed class` | OrderByField.kt | No | ✅ |
| 2 | `feat(lapis): support random(seed) syntax in query parameters` | OrderByField.kt | No | ✅ |
| 3 | `feat(lapis): add OrderBySpec deserializer for array/object formats` | OrderByField.kt | No | ✅ |
| 4 | `feat(lapis): migrate all code to use OrderBySpec` | CommonSequenceFilters.kt, SiloQuery.kt, SequenceFiltersRequest*.kt | No | ✅ |
| 5 | `feat(lapis): add validation to reject mixed random and field ordering` | OrderByValidator.kt (new), Controller | No | ✅ |
| 6 | `test(lapis): add tests for random with seed functionality` | Test files | No | ✅ |

---

## Testing Strategy

### Manual Testing

**GET Requests:**
```bash
# Should work
curl "http://localhost:8080/sample/details?orderBy=random&limit=10"
curl "http://localhost:8080/sample/details?orderBy=random(42)&limit=10"
curl "http://localhost:8080/sample/details?orderBy=country,date&limit=10"

# Should fail
curl "http://localhost:8080/sample/details?orderBy=country,random&limit=10"
```

**POST Requests:**
```bash
# Field ordering
curl -X POST http://localhost:8080/sample/details \
  -H "Content-Type: application/json" \
  -d '{"orderBy": [{"field": "country"}], "limit": 10}'

# Random without seed
curl -X POST http://localhost:8080/sample/details \
  -H "Content-Type: application/json" \
  -d '{"orderBy": {"random": true}, "limit": 10}'

# Random with seed
curl -X POST http://localhost:8080/sample/details \
  -H "Content-Type: application/json" \
  -d '{"orderBy": {"random": 42}, "limit": 10}'
```

### Verification

1. **Same seed = same results:**
   ```bash
   # These two should return identical results
   curl "http://localhost:8080/sample/details?orderBy=random(42)&limit=100"
   curl "http://localhost:8080/sample/details?orderBy=random(42)&limit=100"
   ```

2. **Different seeds = different results:**
   ```bash
   # These should return different orderings
   curl "http://localhost:8080/sample/details?orderBy=random(42)&limit=100"
   curl "http://localhost:8080/sample/details?orderBy=random(43)&limit=100"
   ```

3. **Caching:**
   - Verify random queries are NOT cached (check logs/metrics)
   - Verify normal queries ARE cached

---

## Open Questions / Decisions

1. **Seed range:** Should we validate/limit the seed value range?
2. **Negative seeds:** Allow negative integers as seeds?
3. **Error messages:** Finalize user-facing error messages
4. **Documentation:** Update OpenAPI/Swagger specs
5. **Changelog:** Add entry for this feature

---

## References

- Previous commit adding RandomizeConfig: c663e2df
- Caching fix commit: (current branch)
- SiloQuery.kt line 56: ORDER_BY_RANDOM_FIELD_NAME constant
- SiloClient.kt line 96: Caching condition
