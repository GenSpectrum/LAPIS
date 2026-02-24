# Advanced Variant Query Test Cases

This directory contains JSON test case files for testing advanced variant query functionality.

## Test Case Format

Each JSON file defines a test case with multiple requests. All requests execute sequentially and all must pass for the test case to succeed. This design allows testing that multiple different expressions yield consistent results when viewed together (e.g., verifying that two equivalent queries return the same count).

```json
{
  "testCaseName": "Description of the test case",
  "description": "Optional detailed description of what this test validates",
  "requests": [
    {
      "requestName": "Name of this specific request",
      "description": "Optional description of this request",
      "lapisRequest": {
        "advancedQuery": "C2453T | C913T"
      },
      "expected": [{ "count": 50 }]
    },
    {
      "requestName": "Equivalent query with different syntax",
      "description": "Should return the same count as the previous request",
      "lapisRequest": {
        "advancedQuery": "!(!C2453T & !C913T)"
      },
      "expected": [{ "count": 50 }]
    }
  ]
}
```

## Adding New Test Cases

1. Create a new `.json` file in this directory
2. Follow the format shown above
3. The test infrastructure will automatically discover and execute your test case
4. Run tests with: `npm test -- --grep "Advanced Variant Query"`

## `spec` file

The file that reads this dir is `advancedVariantQueries.spec.ts`.
