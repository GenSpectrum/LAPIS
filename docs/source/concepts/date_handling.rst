.. _dateHandling:

Date handling
=============

.. note:: Partial dates are currently only supported by the monkeypox instance. For SARS-CoV-2, the ``year`` and ``month`` fields do not exist.

The ``date`` field returns and the ``dateFrom`` and ``dateTo`` parameters expect a string formatted as YYYY-MM-DD (e.g., 2022-05-29). There are however samples for which we do not know the exact date but only a partial date: e.g., only the year or the year and the month. In those cases, the ``date`` is considered as unknown and will return a ``null``. That means that the query ``dateFrom=2022-01-01`` will not return samples for which we do not know the exact date but only that it is from May 2022.

To support partial dates, LAPIS additionally has the fields ``year`` and ``month``. They are returned by the ``details`` endpoint and can be used as an aggregation field (e.g., ``fields=year,month`` is possible). Further, LAPIS offers ``yearFrom``, ``yearTo``, ``yearMonthFrom`` and ``yearMonthTo`` filters. ``yearMonth`` has to be formatted as YYYY-MM. For example, the queries ``yearFrom=2022`` and ``yearMonthFrom=2022-05`` will include all samples from May 2022.


Background
----------

Why is the query ``dateFrom=2022-01-01`` not returning samples from May 2022 that don't have an exact date? The reason is that the following (desirable) property would be violated:

.. code-block:: bash

    For t0 < t1:

    aggregated(dateFrom=t0)
    = aggregated(dateFrom=t0,dateTo=t1) + aggregated(dateFrom=t1+1)
    = sum(aggregated(dateFrom=t0,fields=date))
