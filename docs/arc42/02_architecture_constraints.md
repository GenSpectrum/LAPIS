# Architecture Constraints

We identified the following constraints for our software:

- Developed under an open-source licence. We chose the tooling such that a broad spectrum of developers can in principle
  work on the software.
- Versioned by git.
- Input files for SILO are provided in a specific format:
    - metadata and quality control
    - sequence data (non-aligned nucleotide sequences, aligned nucleotide sequences, aligned AA sequences )
    - reference genome
- LAPIS is backed by a database that understands SILO queries.
- The system is designed to have the best possible performance. This mostly targets SILO, but also in LAPIS, we have to
  keep in mind that we are dealing with potentially large data that we have to serve to the client.
- The target platforms, where SILO and LAPIS are run on, are cloud platforms. We provide executable that can be run on
  Linux and macOS. Windows platforms are out of scope. (constraints RAM, HDD, etc.?, cloud?)
