# Architecture Constraints

We identified the following constraints for our software:

- Developed under an open-source licence. We chose the tooling such that a broad spectrum of developers can in principle
  work on the software.
- Input files for SILO are provided in a specific format:
    - metadata and quality control
    - sequence data (non-aligned nucleotide sequences, aligned nucleotide sequences, aligned AA sequences )
    - reference genome
- LAPIS is backed by a database that understands SILO queries.
- The system is designed to have the best possible performance. This mostly targets SILO, but also in LAPIS, we have to
  keep in mind that we are dealing with potentially large data that we have to serve to the client.
- We provide executable that can be run on Linux and macOS. We don't plan to provide executables for Windows, but
  Windows users can of course resort to running Docker images that we provide.
