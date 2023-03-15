This document follows the [arc42 template](https://arc42.org/).

# Introduction and Goals

This document describes LAPIS (Lightweight API for Sequences),
which is a platform to give easy access to genomic sequence data alongside metadata of the sequenced probes.
It is used to filter potentially large sequence data and return the result to the user through web access,
so that a user can develop their own evaluation of the data.

The initial implementation of LAPIS is specialized for SARS-CoV-2 and is used
by [CoV-Spectrum](https://cov-spectrum.org).
In this approach, we want to develop a generalized API that is configurable for a wider range of pathogens.
The solution consists of two systems, LAPIS and SILO, which are designed to work together,
but they could be operated independently of each other.
SILO serves as a database with a specialized query language (SILO queries),
which could in principle be exchanged by any other database.

This document focuses on LAPIS, but we usually think of it as operated as a single unit with SILO.
We refer to this special configuration as SILO-LAPIS.

The following goals have been established for this system:

| Priority |                                                                                                             |
|----------|-------------------------------------------------------------------------------------------------------------|
| 1        | Provide an API to query large sets of genomic sequence data in a very efficient way.                        |
| 2        | Provide the infrastructure such that other researchers ("maintainers") can easily setup their own instance. |

## Requirements Overview

| Id  | Requirement                             |                                                                                  |
|-----|-----------------------------------------|----------------------------------------------------------------------------------|
| F1  | Create an instance for a given pathogen | Create an instance of the whole system by giving a configuration for a pathogen. |
| F2  | Load data                               | Load sequence data that has to be provided in a defined format.                  |
| F3  | Store data                              | Store data in compressed form.                                                   |
| F4  | Provide web access to data              | Provide endpoints for custom user queries to the data.                           |
| F5  | Provide statistics                      | Provide monitoring and statistics of usage of the system.                        |

## Quality Goals

| Quality Category       | Quality          | Description                                                                                                       | Id   |
|------------------------|------------------|-------------------------------------------------------------------------------------------------------------------|------|
| Usability              | Ease of use      | Ease of use for the user to hand in queries.                                                                      | Q:U1 |
|                        | Ease of use      | Ease of use for maintainers to create a new instance for a new pathogen.                                          | Q:U2 |
|                        | Ease of learning | The queries should be as easy to write as possible. We provide material to assist in learning the query language. | Q:U3 |
|                        |                  |                                                                                                                   | Q:U4 |
| Performance efficiency | Time behaviour   | It is possible to query millions of sequences in less than a second.                                              | Q:P1 |
|                        | Scalability      | Performance (query response time, memory usage) grows at most linearly with the number of stored sequences.       | Q:P2 |
|                        |                  |                                                                                                                   |      |
| Maintainability        | Reusability      | It is possible to use LAPIS with any other database that implements the SILO query language                       | Q:M1 |
|                        | Testability      | SILO-LAPIS is well tested on end to end scope. The tests serve as examples for users and maintainers.             | Q:M2 |

## Stakeholders

| Role                                  | Expectations                                                                                    |
|---------------------------------------|-------------------------------------------------------------------------------------------------|
| Database researcher                   | Can develop new genomic data engineering algorithms for LAPIS.                                  |
| Developer                             | Can fix bugs and add new features to LAPIS.                                                     |
| User (beginner level)                 | Can write simple queries to LAPIS by hand and get a fast result.                                |                      
| User (advanced level/tool developers) | Can write advanced, possibly programmatically generated queries to LAPIS and get a fast result. |    
| Maintainer                            | Can host the software on their own servers for their own pathogen configuration.                |