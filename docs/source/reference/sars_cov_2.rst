.. _referenceSarsCoV2:

Reference: SARS-CoV-2 instance
==============================

.. image:: https://img.shields.io/badge/Instance-SARS--CoV--2-blue

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

See :ref:`responseFormat`


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
- variantQuery (see :ref:`variantQuery`)
- pangoLineage
- nextcladePangoLineage
- nextstrainClade
- gisaidClade
- submittingLab
- originatingLab
- nucMutations
- aaMutations
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

See :ref:`mutationFilters`


Pango lineage filter
~~~~~~~~~~~~~~~~~~~~

See :ref:`pangoLineageQuery`


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
