LAPIS
=====

LAPIS (Lightweight API for Sequences) is an open web application programming interface (API) allowing easy querying of SARS-CoV-2 sequencing data using web links. The core features are:

- Filter sequences by metadata or mutations
- Aggregate data by any metadata field you like
- Get the full metadata
- Get the sequences as FASTA (aligned or unaligned)
- Responses can be formatted as JSON and as CSV

Two instances are publicly available:

- For monkeypox (data from GenBank): https://mpox-lapis.genspectrum.org (:ref:`reference <referenceMonkeypox>`)
- For SARS-CoV-2 (data from GenBank): https://lapis.cov-spectrum.org/open (:ref:`reference <referenceSarsCoV2>`)

If you are hosting an own public instance and would like it to be added to the list, please send us an email or `submit a pull request <https://github.com/cevo-public/LAPIS/blob/rtd/docs/source/index.rst>`_ to change this page.

.. toctree::
   :caption: Tutorials
   :hidden:
   :maxdepth: 5

   For developers and maintainers <tutorials/developers_and_maintainers/index>

.. toctree::
   :caption: Concepts
   :hidden:
   :maxdepth: 5

   concepts/data_versions.rst
   concepts/date_handling.rst
   concepts/variant_query.rst

.. toctree::
   :caption: Reference
   :hidden:
   :maxdepth: 5

   Monkeypox <reference/monkeypox>
   SARS-CoV-2 <reference/sars_cov_2>
