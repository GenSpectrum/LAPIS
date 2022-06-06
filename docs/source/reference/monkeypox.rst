.. _referenceMonkeypox:

Reference: monkeypox instance
=============================

.. image:: https://img.shields.io/badge/Instance-Monkeypox-blue

The API for monkeypox uses all monkeypox data on `NCBI GenBank <https://www.ncbi.nlm.nih.gov/genbank/>`_ and from authors who shared them directly with us. The sequences are aligned with `Nextclade <http://nextclade.vercel.app/>`_.



Overview
--------

The API has the following endpoints related to samples. These endpoints provide different types of data:

- ``/sample/aggregated`` - to get summary data aggregated across samples
- ``/sample/details`` - to get per-sample metadata
- ``/sample/contributors`` - to get author names of the samples
- ``/sample/nuc-mutations`` - to get the common nucleotide mutations (shared by at least 5% of the sequences)
- ``/sample/fasta`` - to get original (unaligned) sequences
- ``/sample/fasta-aligned`` - to get aligned sequences

The API returns a response (data) based on a query to one of the endpoints. You can view a response in your browser, or use the data programmatically.

Query Format
~~~~~~~~~~~~

To query an endpoint, use the web link with prefix
``https://mpox-lapis.genspectrum.org/v1`` and the suffix for the relevant endpoint. In the examples, we only show the suffixes to keep things simple, but a click takes you to the full link in your browser.

**Query example:**
Get the total number of available sequences: `/sample/aggregated <https://mpox-lapis.genspectrum.org/v1/sample/aggregated>`_


Response Format
~~~~~~~~~~~~~~~

See :ref:`responseFormat`


Filters
-------

We can adapt the query to filter to only samples of interest. The syntax for adding filters is ``<attribute1>=<valueA>&<attribute2>=<valueB>``.

All **sample** endpoints can be filtered by the following attributes:

- dateFrom (see :ref:`dateHandling`)
- dateTo
- yearFrom
- yearTo
- yearMonthFrom
- yearMonthTo
- dateSubmittedFrom
- dateSubmittedTo
- region
- country
- division
- host
- variantQuery (see :ref:`variantQuery`)
- clade
- nucMutations (see section "Filter Mutations")

The endpoints ``details``, ``contributors``, ``nuc-mutations``, ``fasta``, and ``fasta-aligned`` can additionally be filtered by these attributes:

- sraAccession
- strain

To determine which values are available for each attribute, see the example in section "Aggregation".


Mutation filters
~~~~~~~~~~~~~~~~

It is possible to filter for nucleotide bases/mutations. Multiple mutations can be provided by specifying a comma-separated list.

A nucleotide mutation has the format ``<position><base>``. A "base" can be one of the four nucleotides ``A``, ``T``, ``C``, and ``G``. It can also be ``-`` for deletion and `N` for unknown.

The ``<base>`` can be omitted to filter for any mutation. You can write a ``.`` for the ``<base>`` to filter for sequences for which it is confirmed that no mutation occurred, i.e., has the same base as the reference genome at the specified position.


Aggregation
-----------

Above, we used the ``/sample/aggregated`` endpoint to get the total counts of sequences with or without filters. Using the query parameter ``fields``, we can group the samples and get the counts per group. For example, we can use it to get the number of samples per country. We can also use it to list the available values for each attribute.

``fields`` accepts a comma-separated list. The following values are available:

- date (see :ref:`dateHandling`)
- year
- month
- dateSubmitted
- region
- country
- division
- host
- clade


.. toctree::
   :maxdepth: 3
