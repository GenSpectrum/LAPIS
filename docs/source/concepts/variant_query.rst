.. _variantQuery:

Variant query
=============

LAPIS offers a special query language to specify variants. A variant query can be used to filter sequences and be passed to the server through the query parameter ``variantQuery``. It is not allowed to use the ``variantQuery`` parameter alongside other variant-defining parameters (e.g., ``pangoLineage`` or ``aaMutations``). Don't forget to encode/escape the query correctly (in JavaScript, this can be done with the `"encodeURIComponent()" <https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/encodeURIComponent>`_ function)!

The formal specification of the query language is available `here <https://github.com/cevo-public/LAPIS/blob/main/server/src/main/antlr/ch/ethz/lapis/api/parser/VariantQuery.g4>`_ as an ANTLR v4 grammar. In following, we provide an informal description and examples.

The query language understands Boolean logic. Expressions can be connected with ``&`` (and), ``|`` (or) and ``!`` (not). Parentheses ``(``  and ``)`` can be used to define the order of the operations. Further, there is a special syntax to match sequences for which at least or exactly ``n`` out of a list of expressions are fulfilled.

Examples
--------

Get the sequences with the nucleotide mutation 300G, without a deletion at position 400 and either the AA change S:123T or the AA change S:234A:

.. code-block:: bash

	300G & !400- & (S:123T | S:234A)


Get the sequences with at least 3 out of five mutations/deletions:

.. code-block:: bash

	[3-of: 123A, 234T, S:345-, ORF1a:456K, ORF7:567-]


Get the sequences that fulfill exactly 2 out of 4 conditions:

.. code-block:: bash

	[exactly-2-of: 123A & 234T, !234T, S:345- | S:346-, [2-of: 222T, 333G, 444A, 555C]]


For |sc2-only|, it is also possible to use pango lineage queries (either called by pangolin or by Nextclade) and filter by Nextstrain clades:

.. code-block:: bash

	BA.5* | nextcladePangoLineage:BA.5* | nextstrainClade:22B


.. |sc2-only| image:: https://img.shields.io/badge/Instance-SARS--CoV--2-blue
