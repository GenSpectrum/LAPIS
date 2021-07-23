---
title: API Reference

language_tabs: # must be one of https://git.io/vQNgJ
  - r
  - python

toc_footers:
  - Github (add link)
  - Publication (add link)
  - <a href='https://bsse.ethz.ch/cevo' target="_blank">Developed by cEvo@ETH Zurich</a>
  - <a href='https://github.com/slatedocs/slate' target="_blank">Documentation Powered by Slate</a>

includes:
  - use-cases

search: true

code_clipboard: true
---

# Introduction

*WHHYY am I here?* You'll see:

Y is an open web API allowing easy querying of SARS-CoV-2 sequencing data. The core features are:

- Filter sequences by various metadata as well as by mutations
- Aggregate data by any field you like
- Get the unaggregated data
- Get the raw sequences as fasta (aligned or unaligned)

This instance uses fully public data from [NCBI GenBank](https://www.ncbi.nlm.nih.gov/genbank/) pre-proceessed and hosted by [Nextstrain](https://nextstrain.org/blog/2021-07-08-ncov-open-announcement).


More information about the underlying software and the code can be found in our Github repository (add link). In following, we first show the basic syntax of the API and provide examples on the right side. In the section "Use Cases", we then present examples how this can be used in Python and R. You can switch the programming language in the top-right navigation.


# Overview

> **Examples:**
>
> Get the total number of available sequences:<br/>
> <a href='https://cov-spectrum.ethz.ch/public/api/v1/sample/aggregated' target="_blank">
>   /v1/sample/aggregated
> </a>
>
> Output:

```json
{
  "info":{"apiVersion":1,"deprecationDate":null,"deprecationInfo":null},
  "errors":[],
  "payload":[{"count":913515}]
}
```


There are four main **sample** endpoints:

- `/v1/sample/aggregated` - provides aggregated data
- `/v1/sample/details` - provides detailed information about the samples
- `/v1/sample/fasta` - provides the original (unaligned) sequences in the fasta format
- `/v1/sample/fasta-aligned` - provides the aligned sequences in the fasta format


## Response Format

The responses are in the JSON format. They have three top level attributes:

- info: general information about the API
- errors: something went wrong! See error section (todo) for further details and don't use the results.
- payload: the actual results




# Filters

> **Examples:**
>
> Get the number of all samples in Switzerland in 2021:<br/>
> <a href='https://cov-spectrum.ethz.ch/public/api/v1/sample/aggregated?country=Switzerland&dateFrom=2021-01-01&dateTo=2021-12-31' target="_blank">
>   /v1/sample/aggregated?country=Switzerland&dateFrom=2021-01-01&dateTo=2021-12-31
> </a>

```json
{
  "info":{"apiVersion":1,"deprecationDate":null,"deprecationInfo":null},
  "errors":[],
  "payload":[{"count":22701}]
}
```

> Get details about the AY.1 samples in Geneva, Switzerland:<br/>
> <a href='https://cov-spectrum.ethz.ch/public/api/v1/sample/details?country=Switzerland&division=Geneva&pangoLineage=AY.1' target="_blank">
>   /v1/sample/details?country=Switzerland&division=Geneva&pangoLineage=AY.1
> </a>

```json
{
  "info": {"apiVersion":1,"deprecationDate":null,"deprecationInfo":null},
  "errors": [],
  "payload": [
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
      "gisaidEpiIsl": "EPI_ISL_2405325"
    },
    ...
  ]
}
```

> Get the aligned sequences of the AY.1 samples in Geneva, Switzerland:<br/>
> <a href='https://cov-spectrum.ethz.ch/public/api/v1/sample/fasta-aligned?country=Switzerland&division=Geneva&pangoLineage=AY.1' target="_blank">
>   /v1/sample/fasta-aligned?country=Switzerland&division=Geneva&pangoLineage=AY.1
> </a>


All the main **sample** endpoints can be filtered by the following attributes:

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
- pangoLineage
- nextstrainClade
- gisaidClade
- submittingLab
- originatingLab
- nucMutations

In addition, the endpoints `details`, `fasta`, and `fasta-aligned` can be filtered by these attributes:

- genbankAccession
- sraAccession
- gisaidEpiIsl



# Aggregation

> **Examples:**
>
> Get the number of B.1.1.7 samples per country:<br/>
> <a href='https://cov-spectrum.ethz.ch/public/api/v1/sample/aggregated?fields=country&pangoLineage=B.1.1.7' target="_blank">
>   /v1/sample/aggregated?fields=country&pangoLineage=B.1.1.7
> </a>

```json
{
  "info": {"apiVersion":1,"deprecationDate":null,"deprecationInfo":null},
  "errors": [],
  "payload": [
    {"country": "Austria", "count": 82},
    {"country": "Bahrain", "count": 48},
    ...
  ]
}
```

> Get the number of samples per Nextstrain clade and country:<br/>
> <a href='https://cov-spectrum.ethz.ch/public/api/v1/sample/aggregated?fields=nextstrainClade,country' target="_blank">
>   /v1/sample/aggregated?fields=nextstrainClade,country
> </a>

```json
{
  "info": {"apiVersion":1,"deprecationDate":null,"deprecationInfo":null},
  "errors": [],
  "payload": [
    {"nextstrainClade": "19A", "country": "Australia", "count": 317},
    {"nextstrainClade": "19A", "country": "Bahrain", "count": 2},
    ...
  ]
}
```


Above, we used the `/v1/sample/aggregated` endpoint to get the total counts of sequences with or without filters. Using the query parameter `fields`, we can group the samples and get the counts per group. For example, we can use it to get the number of samples per country.

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
- nextstrainClade
- gisaidClade
- submittingLab
- originatingLab


