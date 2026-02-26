# Benchmarks to test the llms.txt

This is for manual execution.
Copy-paste the following prompts into your LLM agent and see whether it produces the expected results.

## General instructions

```
LAPIS is running on localhost:8090. Look at its `/llms.txt` endpoint.
I will ask you questions about the underlying data next. Tell me when you're ready.
```

## Questions

```
How many sequences are in the dataset?
```

**Expected answer:** 100

```
How many sequences stem from B.1.1.7 including sublineages?
```

**Expected answer:** 51 (48 from B.1.1.7 + 3 from Q.7)

```
Download all sequences of the genes M and N of patients that were younger than 10 in a single JSON file.
```

**Expected answer:** Something along the lines of:

1. Query LAPIS API

- Endpoint: POST /sample/alignedAminoAcidSequences
- Filters: ageTo: 9 (patients younger than 10)
- Parameters: genes: ["M", "N"], dataFormat: "json"
- Expected result: 4 sequences with M and N gene data

2. Save to JSON file

- Filename: sequences_young_patients_M_N.json
- Format: Array of objects, each containing:
  - primaryKey: sequence identifier
  - M: aligned amino acid sequence for M gene
  - N: aligned amino acid sequence for N gene

```
Which nucleotide insertion occurs most often?
```

**Expected answer:** ins_25701:CCC (17 times)

```
Which mutation occurs most often on the S gene?
```

**Expected answer:** S:D614G (98 times)
