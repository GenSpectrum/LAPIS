Reference: monkeypox instance
=============================

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

The API returns a response (data) based on a query to one of the endpoints. You can view a response in your browser, or use the data programmatically. We'll provide some examples in R.

Query Format
~~~~~~~~~~~~

To query an endpoint, use the web link with prefix
``https://mpox-lapis.gen-spectrum.org/v1`` and the suffix for the relevant endpoint. In the examples, we only show the suffixes to keep things simple, but a click takes you to the full link in your browser.

**Query example:**
Get the total number of available sequences: `/sample/aggregated <https://mpox-lapis.gen-spectrum.org/v1/sample/aggregated>`_


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

- dateFrom (see section "Date handling")
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

- date (see section "Date handling")
- year
- month
- dateSubmitted
- region
- country
- division
- host
- clade


Date handling
-------------

The ``date`` field returns and the ``dateFrom`` and ``dateTo`` parameters expect a string formatted as YYYY-MM-DD (e.g., 2022-05-29). There are however samples for which we do not know the exact date but only a partial date: e.g., only the year or the year and the month. In those cases, the ``date`` is considered as unknown and will return a ``null``. That means that the query ``dateFrom=2022-01-01`` will not return samples for which we do not know the exact date but only that it is from May 2022.

To support partial dates, LAPIS additionally has the fields ``year`` and ``month``. They are returned by the ``details`` endpoint and can be used as an aggregation field (e.g., ``fields=year,month`` is possible). Further, LAPIS offers ``yearFrom``, ``yearTo``, ``yearMonthFrom`` and ``yearMonthTo`` filters. ``yearMonth`` has to be formatted as YYYY-MM. For example, the queries ``yearFrom=2022`` and ``yearMonthFrom=2022-05`` will include all samples from May 2022.


Background
~~~~~~~~~~

Why is the query ``dateFrom=2022-01-01`` not returning samples from May 2022 that don't have an exact date? The reason is that the following (desirable) property would be violated:

.. code-block:: bash

    For t0 < t1:

    aggregated(dateFrom=t0)
    = aggregated(dateFrom=t0,dateTo=t1) + aggregated(dateFrom=t1+1)
    = sum(aggregated(dateFrom=t0,fields=date))

.. toctree::
   :maxdepth: 3
