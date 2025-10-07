# Lightweight API for Sequences (LAPIS)

**LAPIS** (**L**ightweight **API** for **S**equences) is a webservice for querying genomic sequences.
It is designed for pathogen data to answer genomic epidemiological questions.
Main features include:

* **Data retrieval**: You can download sequences, metadata, alignments, translations, and annotation data.
* **Data aggregation**: You can flexibly group and aggregate sequences
  (e.g., to find out the number of sequences per country over time)
* **Powerful filters**: You can filter by metadata and combinations of mutations.
  It is possible to specify complex filter conditions using Boolean logic.
* **Easy to use**: It is a simple HTTP/REST API (application programming interface).
  For most cases, you can query it just by typing a URL into your browser.
  It can also be called from any programming language.
* **Many output formats**: You can download the metadata and aggregated data as JSON, CSV, or TSV.
  Sequences are provided as FASTA.
* **Very fast**: LAPIS was originally developed to query SARS-CoV-2 sequences
  and, therefore, capable to process millions of sequences efficiently.
  It uses [SILO](https://github.com/GenSpectrum/LAPIS-SILO) as its data query engine.

## Documentation

We host LAPIS instances ourselves.
Note that the documentation and the Swagger UI are tailored to the specific instance.
You can find documentation of one of our LAPIS instances here:

* Documentation: https://lapis.cov-spectrum.org/open/v2/docs/
* Swagger UI: https://lapis.cov-spectrum.org/open/v2/swagger-ui/index.html

### Hosting LAPIS yourself

LAPIS is designed to be configurable to make analysis of genomic data available to a wide range of users.
It is possible to host your own instance of LAPIS and to configure it to your needs.
We want to make it as easy as possible to set up your own instance of LAPIS.
If you have any trouble, feel free to reach out to us.
We are happy to help!

## OpenAPI documentation

The swagger ui is available at `url.to.lapis:<port>/swagger-ui.html`.
It will help you to explore the LAPIS API and to test it interactively.

The openApi documentation is generated per LAPIS instance from the provided config.

The OpenAPI specification is available at `url.to.lapis:<port>/api-docs` (in JSON format) or at
`url.to.lapis:<port>/api-docs.yaml` (in YAML format).

## SILO Compatibility

This table shows which LAPIS version is required for which SILO version.
Higher versions will also work if they are not specified in the table.

| LAPIS  | SILO   |
|--------|--------|
| 0.5.19 | 0.9.0  |
| 0.5.4  | 0.7.0  |
| 0.3.14 | 0.5.3  |
| 0.3.13 | 0.5.2  |
| 0.3.7  | 0.3.0  |
| 0.2.10 | 0.2.14 |
| 0.2.1  | 0.2.0  |
| 0.1    | 0.1.0  |

## Repository Structure

* `lapis`: The code for [LAPIS](lapis/README.md).
* `lapis-docs`: The [documentation website](lapis-docs/README.md) for LAPIS.
  It is an optional component that can be hosted alongside LAPIS to provide more comprehensive documentation.
* `lapis-e2e`: The end-to-end tests for LAPIS.
  Check the tests if you are looking for example queries, e.g. [here](lapis-e2e/test/aggregatedQueries).

## Contributing

We welcome contributions to LAPIS!
Please check the [contributing guide](CONTRIBUTING.md) for more information.
