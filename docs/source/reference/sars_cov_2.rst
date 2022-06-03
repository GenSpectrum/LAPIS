Reference: SARS-CoV-2 instance
==============================

The API for SARS-CoV-2 uses all SARS-CoV-2 data on `NCBI GenBank <https://www.ncbi.nlm.nih.gov/genbank/>`_ The sequences were pre-processed by `Nextstrain <https://nextstrain.org/blog/2021-07-08-ncov-open-announcement>`_.

Overview
--------

The API has the following endpoints related to samples. These endpoints provide different types of data:

- ``/sample/aggregated`` - to get summary data aggregated across samples
- ``/sample/details`` - to get per-sample metadata
- ``/sample/contributors`` - to get author names of the samples
- ``/sample/aa-mutations`` - to get the common amino acid mutations
- ``/sample/nuc-mutations`` - to get the common nucleotide mutations
- ``/sample/fasta`` - to get original (unaligned) sequences
- ``/sample/fasta-aligned`` - to get aligned sequences

The API returns a response (data) based on a query to one of the endpoints. You can view a response in your browser, or use the data programmatically.


Query Format
~~~~~~~~~~~~

To query an endpoint, use the web link with prefix
``https://lapis.cov-spectrum.org/open/v1`` and the suffix for the relevant endpoint. In the examples, we only show the suffixes to keep things simple, but a click takes you to the full link in your browser.

**Query example:**
Get the total number of available sequences: `/sample/aggregated <https://lapis.cov-spectrum.org/open/v1/sample/aggregated>`_


Response Format
~~~~~~~~~~~~~~~

The responses can be formatted in JSON or CSV. The default is JSON. To get CSV responses, append the query parameter ``dataFormat=csv``.

Responses returned in the `JSON <https://www.json.org/json-en.html>`_ format have three top level attributes:

- "info" - data about the API itself
- "errors" - an array (hopefully empty!) of things that went wrong
- "data" - the actual resposne data


**Response example**:

.. code-block:: json

    {
      "info":{"apiVersion":1,"dataVersion":1653160874,"deprecationDate":null,"deprecationInfo":null,"acknowledgement":null},
      "errors":[],
      "data":[{"count":84}]
    }


Filters
-------

We can adapt the query to filter to only samples of interest. The syntax for adding filters is ``<attribute1>=<valueA>&<attribute2>=<valueB>``.

All **sample** endpoints can be filtered by the following attributes:

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

The endpoints ``details``, ``contributors``, ``nuc-mutations``, ``fasta``, and ``fasta-aligned`` can additionally be filtered by these attributes:

- genbankAccession
- sraAccession
- gisaidEpiIsl
- strain

To determine which values are available for each attribute, see the example in section "Aggregation".


Mutation filters
~~~~~~~~~~~~~~~~

It is possible to filter for nucleotide bases/mutations. Multiple mutations can be provided by specifying a comma-separated list.

A nucleotide mutation has the format ``<position><base>``. A "base" can be one of the four nucleotides ``A``, ``T``, ``C``, and ``G``. It can also be ``-`` for deletion and `N` for unknown.

An amino acid mutation has the format ``<gene>:<position><base>``. The following genes are available: E, M, N, ORF1a, ORF1b, ORF3a, ORF6, ORF7a, ORF7b, ORF8, ORF9b, S. A "base" can be one of the 20 amino acid codes. It can also be ``-`` for deletion and ``X`` for unknown.

The `<base>` can be omitted to filter for any mutation. You can write a `.` for the `<base>` to filter for sequences for which it is confirmed that no mutation occurred, i.e., has the same base as the reference genome at the specified position.


Pango lineage filter
~~~~~~~~~~~~~~~~~~~~

Pango lineage names inherit the hierarchical nature of genetic lineages. For example, B.1.1 is a sub-lineage of B.1. More information about the pango nomenclature can be found on the website of the `Pango network <https://www.pango.network/>`_.

With the ``pangoLineage`` filter, it is possible to not only filter for a very specific lineage but also to include its sub-lineages. To include sub-lineages, add a ``*`` at the end. For example, writing B.1.351 will only give samples of B.1.351. Writing B.1.351* or B.1.351.* (there is no difference between these two options) will return B.1.351, B.1.351.1, B.1.351.2, etc.

An official pango lineage name can only have at most three number components. A sub-lineage of a lineage with a maximal-length name (e.g., B.1.617.2) will get an alias. A list of aliases can be found `here <https://github.com/cov-lineages/pango-designation/blob/master/pango_designation/alias_key.json>`_. B.1.617.2 has the alias AY so that AY.1 would be a sub-lineage of B.1.617.2. LAPIS is aware of aliases. Filtering B.1.617.2* will include every lineage that starts with AY. It is further possible to search for B.1.617.2.1 which will then return the same results as AY.1.


Aggregation
-----------

Above, we used the ``/sample/aggregated`` endpoint to get the total counts of sequences with or without filters. Using the query parameter ``fields``, we can group the samples and get the counts per group. For example, we can use it to get the number of samples per country. We can also use it to list the available values for each attribute.

``fields`` accepts a comma-separated list. The following values are available:

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
