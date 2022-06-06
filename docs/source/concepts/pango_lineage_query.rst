Pango lineage query
===================

.. image:: https://img.shields.io/badge/Instance-SARS--CoV--2-blue

Pango lineage names inherit the hierarchical nature of genetic lineages. For example, B.1.1 is a sub-lineage of B.1. More information about the pango nomenclature can be found on the website of the `Pango network <https://www.pango.network/>`_.

With the ``pangoLineage`` filter and in :ref:`variant queries <variantQuery>`, it is possible to not only filter for a very specific lineage but also to include its sub-lineages. To include sub-lineages, add a ``*`` at the end. For example, writing B.1.351 will only give samples of B.1.351. Writing B.1.351* or B.1.351.* (there is no difference between these two options) will return B.1.351, B.1.351.1, B.1.351.2, etc.

An official pango lineage name can only have at most three number components. A sub-lineage of a lineage with a maximal-length name (e.g., B.1.617.2) will get an alias. A list of aliases can be found `here <https://github.com/cov-lineages/pango-designation/blob/master/pango_designation/alias_key.json>`_. B.1.617.2 has the alias AY so that AY.1 would be a sub-lineage of B.1.617.2. LAPIS is aware of aliases. Filtering B.1.617.2* will include every lineage that starts with AY. It is further possible to search for B.1.617.2.1 which will then return the same results as AY.1.
