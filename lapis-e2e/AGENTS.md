# LAPIS E2E Tests - Development Guide for AI Coding Agents

This guide covers the LAPIS end-to-end integration tests (TypeScript/Mocha).

## Build, Test, and Lint Commands

```bash
cd lapis-e2e

# Setup
npm install
./generateOpenApiClients.sh       # Generate TypeScript client from OpenAPI spec

# Testing
npm test                          # Run all E2E tests
npm test -- --grep "aggregated"   # Run tests matching pattern
npm test -- --grep "mutations"    # Run mutation-related tests

# Code quality
npm run check-format              # Check Prettier formatting (CI enforces this)
npm run format                    # Auto-fix formatting
```

## Purpose and Scope

E2E tests verify:

1. **Integration** - LAPIS and SILO working together end-to-end
2. **API Contract** - Endpoints match OpenAPI specification
3. **Data Correctness** - Responses contain expected data
4. **Client Generation** - Generated TypeScript client works correctly

These tests run against a real LAPIS instance with SILO backend.

## Project Structure

```
lapis-e2e/
├── test/                         # Test files
│   ├── lapisClient/              # Auto-generated OpenAPI client
│   ├── aggregated.spec.ts        # Aggregated endpoint tests
│   ├── mutations.spec.ts         # Mutation tests
│   ├── common.ts                 # Reusable test utilities
│   └── ...
├── testData/                     # Test fixtures and expected results
└── package.json
```

## Code Style Guidelines

### Code Patterns

- **Arrow functions** preferred over function declarations
- **const** for all variables (avoid let, never use var)
- **Interface definitions** for types
- **Async/await** for promises (not .then() chains)
- **Destructuring** for object properties

Example:

```typescript
// Good
const fetchData = async (endpoint: string): Promise<ApiResponse> => {
  const response = await fetch(endpoint);
  return response.json();
};

// Avoid
function fetchData(endpoint: string): Promise<ApiResponse> {
  return fetch(endpoint).then(response => response.json());
}
```

## Testing Framework

### Mocha + Chai

- **Mocha:** Test runner with BDD-style syntax
- **Chai:** Assertion library with expect syntax

```typescript
import { expect } from 'chai';
import { describe, it } from 'mocha';

describe('The /aggregated endpoint', () => {
  it('should return data for the test case', async () => {
    const result = await lapisClient.postAggregated({
      aggregatedPostRequest: testCase.lapisRequest,
    });
    expect(result.data).to.have.deep.members(testCase.expected);
  });
});
```

## Test Structure

### Basic Test Pattern

```typescript
describe('Feature or Endpoint', () => {
  it('should do something specific', async () => {
    // Arrange - Set up test data
    const request = {
      country: 'USA',
      dateFrom: '2023-01-01',
    };

    // Act - Execute the operation
    const result = await lapisClient.postAggregated({
      aggregatedPostRequest: request,
    });

    // Assert - Verify the result
    expect(result.data).to.have.lengthOf(1);
    expect(result.data[0].count).to.equal(12345);
  });
});
```

### Using Generated Client

Ready-to-use clients are defined in the `common.ts`.
Prefer the generated client over raw fetch:

```typescript
// Good - Use generated client
const result = await lapisClient.postAggregated({
  aggregatedPostRequest: { country: 'USA' },
});

// Only use fetch for edge cases not supported by client
const response = await fetch('http://localhost:8080/aggregated', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ country: 'USA' }),
});
```

### Data-Driven Tests

```typescript
interface TestCase {
  name: string;
  lapisRequest: AggregatedPostRequest;
  expected: AggregatedResponse[];
}

const testCases: TestCase[] = [
  {
    name: 'filter by country',
    lapisRequest: { country: 'USA' },
    expected: [{ country: 'USA', count: 100 }],
  },
  {
    name: 'filter by date range',
    lapisRequest: { dateFrom: '2023-01-01', dateTo: '2023-12-31' },
    expected: [{ count: 500 }],
  },
];

describe('Aggregated endpoint', () => {
  testCases.forEach(testCase => {
    it(`should handle ${testCase.name}`, async () => {
      const result = await lapisClient.postAggregated({
        aggregatedPostRequest: testCase.lapisRequest,
      });
      expect(result.data).to.have.deep.members(testCase.expected);
    });
  });
});
```

## Common Assertions

```typescript
// Equality
expect(result).to.equal(42);
expect(result).to.deep.equal({ foo: 'bar' });

// Arrays
expect(array).to.have.lengthOf(5);
expect(array).to.include('item');
expect(array).to.have.deep.members([{ id: 1 }, { id: 2 }]);

// Objects
expect(obj).to.have.property('key');
expect(obj).to.have.property('key', 'value');
expect(obj).to.include({ key: 'value' });

// Types
expect(value).to.be.a('string');
expect(value).to.be.an('array');

// Booleans
expect(value).to.be.true;
expect(value).to.be.false;

// Null/Undefined
expect(value).to.be.null;
expect(value).to.be.undefined;
expect(value).to.exist;
```

## Client Generation

### Generate Client from OpenAPI Spec

```bash
./generateOpenApiClients.sh
```

This command:

1. Temporarily starts several LAPIS instances to fetch OpenAPI spec
2. Generates TypeScript client using openapi-generator
3. Places generated code in subdirectories of `test/` for each generated client
4. Client provides type-safe API calls

### When to Regenerate

- After adding/modifying LAPIS endpoints
- After changing request/response schemas
- After updating OpenAPI annotations
- When tests fail due to type mismatches

## Running Tests

### Run All Tests

```bash
npm test
```

### Run Specific Tests

```bash
# By pattern
npm test -- --grep "aggregated"

# By file
npm test test/aggregated.spec.ts

# Multiple patterns
npm test -- --grep "aggregated|mutations"
```

## Technology Stack

- **TypeScript:** Type-safe JavaScript
- **Mocha:** Test framework
- **Chai:** Assertion library
- **OpenAPI Generator:** Client code generation
- **Node.js:** Runtime environment
- **npm:** Package manager

## Important Development Notes

### CI/CD Integration

- Tests run in GitHub Actions
- Docker containers spun up for LAPIS and SILO
- Test data loaded automatically
- Prettier formatting enforced
- All tests must pass before merge
