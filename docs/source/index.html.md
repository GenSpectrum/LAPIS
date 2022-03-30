---
title: LAPIS Documentation

language_tabs: # must be one of https://git.io/vQNgJ
  - r

toc_footers:
  - <a href="https://github.com/cevo-public/LAPIS">Github</a>
  - <a href="https://lapis.cov-spectrum.org/openapi/">OpenAPI-specification</a>
  - <a href="https://bsse.ethz.ch/cevo">Developed by cEvo@ETH Zurich</a>
  - <a href="https://github.com/slatedocs/slate">Documentation powered by Slate</a>

includes:
  - use-cases

search: true

code_clipboard: true
---

# Introduction

LAPIS (Lightweight API for Sequences) is an open web application programming interface (API) allowing easy querying of SARS-CoV-2 sequencing data using web links. The core features are:

- Filter sequences by metadata or mutations
- Aggregate data by any metadata field you like
- Get the full metadata
- Get the sequences as FASTA (aligned or unaligned)
- Responses can be formatted as JSON and as CSV

An OpenAPI-specification is available [here](https://lapis.cov-spectrum.org/openapi/).

This instance uses fully public data from [NCBI GenBank](https://www.ncbi.nlm.nih.gov/genbank/) pre-proceessed and hosted by [Nextstrain](https://nextstrain.org/blog/2021-07-08-ncov-open-announcement). We update the data every day. More information about the underlying software and the code can be found in our [Github repository](https://github.com/cevo-public/LAPIS). 

In following, we demostrate the core features enabled by the API. On the left, we present the basic syntax of the API and on the right, we show how to use it for queries. In the section "Use Cases", we provide examples how to use the API to query public SARS-CoV-2 sequencing data to generate statistics, create plots, or download sequences for further analysis. 


# Overview

The API has six main endpoints related to samples. These endpoints provide different types of data:

- `/sample/aggregated` - use to get summary data aggregated across samples
- `/sample/details` - use to get per-sample metadata
- `/sample/aa-mutations` - use to get the common amino acid mutations (shared by at least 5% of the sequences)
- `/sample/nuc-mutations` - use to get the common nucleotide mutations (shared by at least 5% of the sequences)
- `/sample/fasta` - use to get original (unaligned) sequences
- `/sample/fasta-aligned` - use to get aligned sequences

The API returns a response (data) based on a query to one of the endpoints. You can view a response in your browser, or use the data programatically. We'll provide some examples in R.

<aside class="notice">
The <code>/sample/fasta</code> and <code>/sample/fasta-aligned</code> endpoints has a limit of 100,000 sequences per request. We do not support pagination (yet). However, you could, for example, use the <code>dateFrom</code> and <code>dateTo</code> filters to load the sequences chunk by chunk.
</aside>

## Query Format

> **Query example:**
>
> Get the total number of available sequences:<br/>
> <a href='https://lapis.cov-spectrum.org/open/v1/sample/aggregated' target="_blank">
>   /sample/aggregated
> </a>

To query an endpoint, use the web link with prefix
`https://lapis.cov-spectrum.org/open/v1` and the suffix for the relevant endpoint. In the examples, we only show the suffixes to keep things simple, but you can click to try the full link in your browser.

## Response Format

> **Response example**:

```json
{
  "info":{"apiVersion":1,"deprecationDate":null,"deprecationInfo":null},
  "errors":[],
  "data":[{"count":913515}]
}
```

The responses can be formatted in JSON or CSV. The default is JSON. To get CSV responses, append the query parameter `dataFormat=csv`.

Responses returned in the [JSON](https://www.json.org/json-en.html) format have three top level attributes:

- "info" - data about the API itself
- "errors" - an array (hopefully empty!) of things that wrong.
- "data" - the actual data


# Filters

> **Examples:**
>
> Get the number of all samples in Switzerland in 2021:<br/>
> <a href='https://lapis.cov-spectrum.org/open/v1/sample/aggregated?country=Switzerland&dateFrom=2021-01-01&dateTo=2021-12-31' target="_blank">
>   /sample/aggregated?country=Switzerland&dateFrom=2021-01-01&dateTo=2021-12-31
> </a>

```json
{
  "info":{"apiVersion":1,"deprecationDate":null,"deprecationInfo":null},
  "errors":[],
  "data":[{"count":22701}]
}
```

> Get details about samples from lineage AY.1 in Geneva, Switzerland:<br/>
> <a href='https://lapis.cov-spectrum.org/open/v1/sample/details?country=Switzerland&division=Geneva&pangoLineage=AY.1' target="_blank">
>   /sample/details?country=Switzerland&division=Geneva&pangoLineage=AY.1
> </a>

```json
{
  "info": {"apiVersion":1,"deprecationDate":null,"deprecationInfo":null},
  "errors": [],
  "data": [
    {
      "date": "2021-05-26",
      "dateSubmitted": "2021-06-29",
      "region": "Europe",
      "country": "Switzerland",
      "division": "Geneva",
      "location": null,
      "regionExposure": "Europe",
      "countryExposure": "Switzerland",
      "divisionExposure": "Geneva",
      "age": null,
      "sex": null,
      "host": "Homo sapiens",
      "samplingStrategy": null,
      "pangoLineage": "AY.1",
      "nextstrainClade": "21A (Delta)",
      "gisaidCloade": null,
      "submittingLab": null,
      "originatingLab": null,
      "genbankAccession": "OU268406",
      "sraAccession": null,
      "gisaidEpiIsl": "EPI_ISL_2405325",
      "strain":"Switzerland/GE-HUG-34284688/2021"
    },
    ...
  ]
}
```

Large queries, for example detailed information on all the samples, will take a bit. Instead, we can adapt the query to filter to only samples of interest. The syntax for additing filters is `<attribute1>=<valueA>&<attribute2>=<valueB>`.

All six **sample** endpoints can be filtered by the following attributes:

- dateFrom
- dateTo
- dateSubmittedFrom
- dateSubmittedTo
- region
- country
- division
- location
- regionExposure
- countryExposure
- divisionExposure
- ageFrom
- ageTo
- sex
- host
- samplingStrategy
- pangoLineage (see section "Filter Pango Lineages")
- nextcladePangoLineage
- nextstrainClade
- gisaidClade
- submittingLab
- originatingLab
- nucMutations (see section "Filter Mutations")
- aaMutations (see section "Filter Mutations")
- nextcladeQcOverallScoreFrom
- nextcladeQcOverallScoreTo
- nextcladeQcMissingDataScoreFrom
- nextcladeQcMissingDataScoreTo
- nextcladeQcMixedSitesScoreFrom
- nextcladeQcMixedSitesScoreTo
- nextcladeQcPrivateMutationsScoreFrom
- nextcladeQcPrivateMutationsScoreTo
- nextcladeQcSnpClustersScoreFrom
- nextcladeQcSnpClustersScoreTo
- nextcladeQcFrameShiftsScoreFrom
- nextcladeQcFrameShiftsScoreTo
- nextcladeQcStopCodonsScoreFrom
- nextcladeQcStopCodonsScoreTo

The endpoints `details`, `aa-mutations`, `nuc-mutations`, `fasta`, and `fasta-aligned` can additionally be filtered by these attributes:

- genbankAccession
- sraAccession
- gisaidEpiIsl
- strain

To determine which values are available for each attribute, see the example in section "Aggregation".

## Filter Pango Lineages


> Get the total number of samples of the lineage B.1.617.2 without sub-lineages:<br/>
> <a href="https://lapis.cov-spectrum.org/open/v1/sample/aggregated?pangoLineage=B.1.617.2" target="_blank">
>   /sample/aggregated?pangoLineage=B.1.617.2
> </a>

> Get the total number of samples of the lineage B.1.617.2 including sub-lineages:<br/>
> <a href="https://lapis.cov-spectrum.org/open/v1/sample/aggregated?pangoLineage=B.1.617.2*" target="_blank">
>   /sample/aggregated?pangoLineage=B.1.617.2*
> </a>


Pango lineage names inherit the hierarchical nature of genetic lineages. For example, B.1.1 is a sub-lineage of B.1. More information about the pango nomenclature can be found on the website of the [Pango network](https://www.pango.network/).

With the `pangoLineage` filter, it is possible to not only filter for a very specific lineage but also to include its sub-lineages. To include sub-lineages, add a `*` at the end. For example, writing B.1.351 will only give samples of B.1.351. Writing B.1.351* or B.1.351.* (there is no difference between these two options) will return B.1.351, B.1.351.1, B.1.351.2, etc.

An official pango lineage name can only have at most three number components. A sub-lineage of a lineage with a maximal-length name (e.g., B.1.617.2) will get an alias. A list of aliases can be found [here](https://github.com/cov-lineages/pango-designation/blob/master/pango_designation/alias_key.json). B.1.617.2 has the alias AY so that AY.1 would be a sub-lineage of B.1.617.2. LAPIS is aware of aliases. Filtering B.1.617.2* will include every lineage that starts with AY. It is further possible to search for B.1.617.2.1 which will then return the same results as AY.1.


## Filter Mutations

> Get the total number of samples with the synonymous nucleotide mutations 913T and 5986T and the amino acid mutation S:484K:<br/>
> <a href="https://lapis.cov-spectrum.org/open/v1/sample/aggregated?nucMutations=913T,5986T&aaMutations=S:484K" target="_blank">
>   /sample/aggregated?nucMutations=913T,5986T&aaMutations=S:484K
> </a>

> Get the total number of samples for which we do not know whether the S:501 position is mutated:<br/>
> <a href="https://lapis.cov-spectrum.org/open/v1/sample/aggregated?aaMutations=S:501X" target="_blank">
>   /sample/aggregated?aaMutations=S:501X
> </a>

It is possible to filter for amino acid and nucleotide bases/mutations. Multiple mutations can be provided by specifying a comma-separated list.

A nucleotide mutation has the format `<position><base>`. A "base" can be one of the four nucleotides `A`, `T`, `C`, and `G`. It can also be `-` for deletion and `N` for unknown.

An amino acid mutation has the format `<gene>:<position><base>`. The following genes are available: E, M, N, ORF1a, ORF1b, ORF3a, ORF6, ORF7a, ORF7b, ORF8, ORF9b, S. A "base" can be one of the 20 amino acid codes. It can also be `-` for deletion and `X` for unknown.

The `<base>` can be omitted to filter for any mutation. You can write a `.` for the `<base>` to filter for sequences for which it is confirmed that no mutation occurred, i.e., has the same base as the reference genome at the specified position.


# Aggregation

> **Examples:**
>
> Get the number of B.1.1.7 samples per country:<br/>
> <a href='https://lapis.cov-spectrum.org/open/v1/sample/aggregated?fields=country&pangoLineage=B.1.1.7' target="_blank">
>   /sample/aggregated?fields=country&pangoLineage=B.1.1.7
> </a>

```json
{
  "info": {"apiVersion":1,"deprecationDate":null,"deprecationInfo":null},
  "errors": [],
  "data": [
    {"country": "Austria", "count": 82},
    {"country": "Bahrain", "count": 48},
    ...
  ]
}
```

> Get the number of samples per Nextstrain clade and country:<br/>
> <a href='https://lapis.cov-spectrum.org/open/v1/sample/aggregated?fields=nextstrainClade,country' target="_blank">
>   /sample/aggregated?fields=nextstrainClade,country
> </a>

```json
{
  "info": {"apiVersion":1,"deprecationDate":null,"deprecationInfo":null},
  "errors": [],
  "data": [
    {"nextstrainClade": "19A", "country": "Australia", "count": 317},
    {"nextstrainClade": "19A", "country": "Bahrain", "count": 2},
    ...
  ]
}
```

> Get all the possible values for attribute "division" in Swtizerland:<br/> 
> <a href='https://lapis.cov-spectrum.org/open/v1/sample/aggregated?fields=division&country=Switzerland' target="_blank">
>   /sample/aggregated?division,country=Switzerland
> </a>

```json
{
  "info": {"apiVersion":1,"deprecationDate":null,"deprecationInfo":null},
  "errors": [],
  "data": [
    {"division": "Basel-Land", "count": 4658},
    {"division": "Aargau", "count": 2964},
    ...
  ]
}
```

Above, we used the `/sample/aggregated` endpoint to get the total counts of sequences with or without filters. Using the query parameter `fields`, we can group the samples and get the counts per group. For example, we can use it to get the number of samples per country. We can also use it to list the available values for each attribute.

`fields` accepts a comma-separated list. The following values are available:

- date
- dateSubmitted
- region
- country
- division
- location
- regionExposure
- countryExposure
- divisionExposure
- age
- sex
- host
- samplingStrategy
- pangoLineage
- nextcladePangoLineage
- nextstrainClade
- gisaidClade
- submittingLab
- originatingLab


