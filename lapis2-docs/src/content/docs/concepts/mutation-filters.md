---
title: Mutation filters
description: Mutation filters
---

It is possible to filter for amino acid and nucleotide bases/mutations.
Multiple mutations can be provided by specifying a comma-separated list.

A nucleotide mutation has the format `<position><base>`.
A `<base>` can be one of the four nucleotides `A`, `T`, `C`, and `G`.
It can also be `-` for deletion and `N` for unknown.

An amino acid mutation has the format `<gene>:<position><base>`.
The following genes are available: E, M, N, ORF1a, ORF1b, ORF3a, ORF6, ORF7a, ORF7b, ORF8, ORF9b, S.
A `<base>` can be one of the 20 amino acid codes.
It can also be `-` for deletion and `X` for unknown.

The `<base>` can be omitted to filter for any mutation.
You can write a `.` for the `<base>` to filter for sequences for which it is confirmed that no mutation occurred,
i.e. has the same base as the reference genome at the specified position.
