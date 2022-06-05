.. _responseFormat:

Response format
===============

Most endpoints can return data either as JSON or CSV. The default is JSON. To get a CSV, specify the query parameter ``dataFormat=csv``.

Responses returned in JSON have three top level attributes:

- "info" - data about the API itself
- "errors" - an array (hopefully empty!) of things that went wrong
- "data" - the actual response data

Example:

.. code-block:: json

    {
      "info":{"apiVersion":1,"dataVersion":1653160874,"deprecationDate":null,"deprecationInfo":null,"acknowledgement":null},
      "errors":[],
      "data":[{"count":84}]
    }

Genomic sequences (``fasta`` and ``fasta-aligned`` endpoints) are returned in the `FASTA format <https://en.wikipedia.org/wiki/FASTA_format>`_.

.. note:: Every response, independent of the data format, contains the data version as it is a very important information. See the :ref:`data versions page <dataVersions>` for details.
