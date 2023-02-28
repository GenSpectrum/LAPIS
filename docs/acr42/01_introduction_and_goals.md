This document follows the [arc42 template](https://arc42.org/).
# Introduction and Goals

This document describes LAPIS, which is a platform to give easy access to genomic sequence data alongside metadata of
the sequenced probes. It is used to filter sequence data and return the result to the user through web access, so the
user can develop his/her own evaluation of the data. The current development is based on two existing
systems (LAPIS-api and SILO), which are developed for SarsCov2 viruses, and shall be extended to also include other
virus species.

The following goals have been established for this system:

| Priority |                                                            |
|----------|------------------------------------------------------------|
| 1        | Include current functionality of LAPIS in the new approach |
| 2        | Provide simple setup for new virus species                 |
| 3        |                                                            |

## Requirements Overview

| Id  | Requirement                |                                                                                                      |
|-----|----------------------------|------------------------------------------------------------------------------------------------------|
| F1  | create user instance       | create user instance of the whole system by given configuration for different virus species          |
| F2  | read in data               | read data provided as aligned and non-aligned sequence data for nucleotides and aminoacids, metadata |
| F3  | store data                 | store data in compressed form and give fast access                                                   |
| F4  | provide web access to data | provide endpoint for custom queries to the data                                                      |
| F5  | provide statstics          | provide statistics for usage of system                                                               |

## Quality Goals

| Quality Category       | Quality               | Description                                                                                                                           | Id   |
|------------------------|-----------------------|---------------------------------------------------------------------------------------------------------------------------------------|------|
| Usability              | Ease of use           | Ease of use by the enduser to hand in queries.                                                                                        | Q:U1 |
|                        | Ease of use           | Ease of use to create a new instance for a new virus species.                                                                         | Q:U2 |
|                        | Ease of learning      | The queries should be as easy to write as possible. Easy to learn for developers?                                                     | Q:U3 |
|                        | Understandability     | The queries should be understandable for ...?                                                                                         | Q:U4 |
|                        | User error protection | ...?                                                                                                                                  | Q:U5 |
| Performance efficiency | Time behaviour        | Response time of whole system? Response of database?                                                                                  | Q:P1 |
|                        | Resource utilization  | How many do we have?                                                                                                                  | Q:P2 |
|                        | Workload              | How many requests should be tolerable?                                                                                                | Q:P3 |
|                        | Scalability           | How many sequences do we expect?                                                                                                      | Q:P4 |
|                        |                       |                                                                                                                                       |      |
| Maintainability        | Modularity            | Wie einfach sollen einzelne Komponenten (Web access, database, config) ausgetauscht werden können?                                    | Q:M1 |
|                        | Reusability           | Welche Komponenten sollen auch getrennt benutzt werden können (database?)                                                             | Q:M2 |
|                        | Analysability         | Wie effektiv und effizient soll das fertige Produkt analysiert werden können?                                                         | Q:M3 |
|                        | Modifiability         | Welche Komponten sollen angepasst werden können? The (whole?) software should be easy to modify to cope with different virus species. | Q:M4 |
|                        | Testability           | All single components should be tested. The whole system is tested from starting an instance to the user query.                       | Q:M5 |

[//]: # (| Cultural and Regional? |)

[//]: # (| Legal?)

[//]: # (| Security?)

[//]: # ()

[//]: # (Scenarios:)

## Stakeholders

| Role                              | Expectations                                                           |
|-----------------------------------|------------------------------------------------------------------------|
| Database researcher (Alex)        | ?                                                                      |                                                                      
| Alignment researcher (Cornelius)  | ?                                                                      |                                                                      
| Sequence researcher (Chaoran)     | ?                                                                      |                                                                    
| Financier (Tanja, DFG?)           | ?                                                                      |                                                                   
| Maintainer                        | Can fix bugs and add new features to software.                         |                         
| End-user                          | Can write queries to LAPIS and get a fast result.                      |                      
| Research facility                 | Can host the software on their own servers for their own virus specie. | 