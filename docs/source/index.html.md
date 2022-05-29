---
title: LAPIS for Monkeypox

language_tabs: # must be one of https://git.io/vQNgJ
  - r

toc_footers:
  - <a href="https://github.com/cevo-public/LAPIS">Github</a>
  - <a href="https://bsse.ethz.ch/cevo">LAPIS developed by cEvo@ETH Zurich</a>
  - <a href="https://github.com/slatedocs/slate">Documentation powered by Slate</a>

includes:
  - use-cases
  - about

search: true

code_clipboard: true
---

# Introduction

LAPIS (Lightweight API for Sequences) is an open web application programming interface (API) allowing easy querying of genomic sequencing data. Originally developed for SARS-CoV-2 and used by [CoV-Spectrum](https://cov-spectrum.org), it is now also available for monkeypox. The API for monkeypox uses all monkeypox data on [NCBI GenBank](https://www.ncbi.nlm.nih.gov/genbank/) and from authors who shared them directly with us. The data were pre-proceessed and aligned by the Nextstrain team. We will update the data as soon as they come in. The core features are:

- Filter sequences by metadata or mutations
- Aggregate data by any metadata field you like
- Get the full metadata
- Get the sequences as FASTA (aligned or unaligned)
- Responses can be formatted as JSON and as CSV


In the following, we demostrate the core features enabled by the API. On the left, we present the basic syntax of the API and on the right, we show how to use it for queries. In the section "Use Cases", we provide examples how to use the API to generate statistics, create plots, or download sequences for further analysis based on the publically available monkeypox sequencing data.

<aside class="notice">
The aim of the API is to allow people to access monkeypox sequencing data and corresponding metadata programmatically and to build tools and dashboards on top of it, without the need to handle data updates and pre-processing themselves. If you decide to use LAPIS in your software, feel free to send us a short email so that we can keep you updated about important updates.
</aside>


# Overview

The API has five main endpoints related to samples. These endpoints provide different types of data:

- `/sample/aggregated` - to get summary data aggregated across samples
- `/sample/details` - to get per-sample metadata
- `/sample/nuc-mutations` - to get the common nucleotide mutations (shared by at least 5% of the sequences)
- `/sample/fasta` - to get original (unaligned) sequences
- `/sample/fasta-aligned` - to get aligned sequences

The API returns a response (data) based on a query to one of the endpoints. You can view a response in your browser, or use the data programmatically. We'll provide some examples in R.

## Query Format

> **Query example:**
>
> Get the total number of available sequences:<br/>
> <a href='https://mpox-lapis.gen-spectrum.org/v1/sample/aggregated' target="_blank">
>   /sample/aggregated
> </a>

To query an endpoint, use the web link with prefix
`https://mpox-lapis.gen-spectrum.org/v1` and the suffix for the relevant endpoint. In the examples, we only show the suffixes to keep things simple, but a click takes you to the full link in your browser.

## Response Format

> **Response example**:

```json
{
  "info":{"apiVersion":1,"dataVersion":1653160874,"deprecationDate":null,"deprecationInfo":null,"acknowledgement":null},
  "errors":[],
  "data":[{"count":84}]
}
```

The responses can be formatted in JSON or CSV. The default is JSON. To get CSV responses, append the query parameter `dataFormat=csv`.

Responses returned in the [JSON](https://www.json.org/json-en.html) format have three top level attributes:

- "info" - data about the API itself
- "errors" - an array (hopefully empty!) of things that went wrong
- "data" - the actual resposne data


# Filters

> **Examples:**
>
> Get the number of all samples from Nigeria since 2000:<br/>
> <a href='https://mpox-lapis.gen-spectrum.org/v1/sample/aggregated?country=Nigeria&dateFrom=2000-01-01' target="_blank">
>   /sample/aggregated?country=Nigeria&dateFrom=2000-01-01
> </a>

```json
{
  "info":...,
  "errors":[],
  "data":[{"count":5}]
}
```

> Get metadata of samples of the West Africa clade:<br/>
> <a href='https://mpox-lapis.gen-spectrum.org/v1/sample/details?clade=WA' target="_blank">
>   /sample/details?clade=WA
> </a>

```json
{
  "info": ...,
  "errors": [],
  "data": [
    {
      "date":"2017-11-30",
      "country":"Nigeria",
      "host":"human",
      "clade":"WA",
      "sraAccession":"MK783030",
      "strain":"3025",
      ...
    },
    ...
  ]
}
```

We can adapt the query to filter to only samples of interest. The syntax for adding filters is `<attribute1>=<valueA>&<attribute2>=<valueB>`.

All five **sample** endpoints can be filtered by the following attributes:

- dateFrom (see section "Date handling")
- dateTo
- yearFrom
- yearTo
- yearMonthFrom
- yearMonthTo
- region
- country
- division
- host
- clade
- nucMutations (see section "Filter Mutations")

The endpoints `details`, `nuc-mutations`, `fasta`, and `fasta-aligned` can additionally be filtered by these attributes:

- sraAccession
- strain

To determine which values are available for each attribute, see the example in section "Aggregation".


## Filter Mutations

> Get the total number of samples with the nucleotide mutations 913T and 5986T:<br/>
> <a href="https://mpox-lapis.gen-spectrum.org/v1/sample/aggregated?nucMutations=913T,5986T" target="_blank">
>   /sample/aggregated?nucMutations=913T,5986T
> </a>

It is possible to filter for nucleotide bases/mutations. Multiple mutations can be provided by specifying a comma-separated list.

A nucleotide mutation has the format `<position><base>`. A "base" can be one of the four nucleotides `A`, `T`, `C`, and `G`. It can also be `-` for deletion and `N` for unknown.

The `<base>` can be omitted to filter for any mutation. You can write a `.` for the `<base>` to filter for sequences for which it is confirmed that no mutation occurred, i.e., has the same base as the reference genome at the specified position.


# Aggregation

> **Examples:**
>
> Get the number of samples per country:<br/>
> <a href='https://mpox-lapis.gen-spectrum.org/v1/sample/aggregated?fields=country' target="_blank">
>   /sample/aggregated?fields=country
> </a>

```json
{
  "info": {"apiVersion":1,"deprecationDate":null,"deprecationInfo":null},
  "errors": [],
  "data": [
    {"country":"France","count":1},
    {"country":"Portugal","count":1}
    ...
  ]
}
```

> Get the number of samples per host and country from the 2022:<br/>
> <a href='https://mpox-lapis.gen-spectrum.org/v1/sample/aggregated?dateFrom=2022-01-01&fields=host,country' target="_blank">
>   /sample/aggregated?dateFrom=2022-01-01&fields=host,country
> </a>

```json
{
  "info": {"apiVersion":1,"deprecationDate":null,"deprecationInfo":null},
  "errors": [],
  "data": [
    {"host":"human","country":"USA","count":1},
    {"host":"human","country":"Portugal","count":1},
    ...
  ]
}
```

Above, we used the `/sample/aggregated` endpoint to get the total counts of sequences with or without filters. Using the query parameter `fields`, we can group the samples and get the counts per group. For example, we can use it to get the number of samples per country. We can also use it to list the available values for each attribute.

`fields` accepts a comma-separated list. The following values are available:

- date (see section "Date handling")
- year
- month
- region
- country
- division
- host
- clade


# Date handling

The `date` field returns and the `dateFrom` and `dateTo` parameters expect a string formatted as YYYY-MM-DD (e.g., 2022-05-29). There are however samples for which we do not know the exact date but only a partial date: e.g., only the year or the year and the month. In those cases, the `date` is considered as unknown and will return a `null`. That means that the query `dateFrom=2022-01-01` will not return samples for which we do not know the exact date but only that it is from May 2022.

To support partial dates, LAPIS additionally has the fields `year` and `month`. They are returned by the `details` endpoint and can be used as an aggregation field (e.g., `fields=year,month` is possible). Further, LAPIS offers `yearFrom`, `yearTo`, `yearMonthFrom` and `yearMonthTo` filters. `yearMonth` has to be formatted as YYYY-MM. For example, the queries `yearFrom=2022` and `yearMonthFrom=2022-05` will include all samples from May 2022.


## Background

Why is the query `dateFrom=2022-01-01` not returning samples from May 2022 that don't have an exact date? The reason is that the following (desirable) property would be violated:

For t0 < t1:

aggregated(dateFrom=t0)
= aggregated(dateFrom=t0,dateTo=t1) + aggregated(dateFrom=t1+1)
= sum(aggregated(dateFrom=t0,fields=date))
