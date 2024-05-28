# Lightweight API for Sequences (LAPIS)

This is a generalized Lightweight API for Sequences. It uses [SILO](https://github.com/GenSpectrum/LAPIS-SILO) as
database for storing and querying the sequence data.

You can find the code for LAPIS in the `lapis` directory, the documentation for LAPIS in the `lapis-docs` directory, and
the end-to-end tests in the `lapis-e2e` directory.

## OpenAPI documentation

The openApi documentation is generated per LAPIS instance from the provided config.

The swagger ui is available at `url.to.lapis:<port>/swagger-ui.html`.

The OpenAPI specification is available at `url.to.lapis:<port>/api-docs` (in JSON format) or at
`url.to.lapis:<port>/api-docs.yaml` (in YAML format).

## Creating A Release

This project uses [Release Please](https://github.com/google-github-actions/release-please-action) to generate releases.
On every commit on the `main` branch, it will update a Pull Request with a changelog.
When the PR is merged, the release will be created.
Creating a release means:

* A new Git tag is created.
* The Docker images of lapis and lapis-docs are tagged with the new version.
    * Suppose the created version is `2.4.5`, then it creates the tags `2`, `2.4` and `2.4.5`.

The changelog and the version number are determined by the commit messages.
Therefore, commit messages should follow the [Conventional Commits](https://www.conventionalcommits.org/) specification.
Also refer to the Release Please documentation for more information on how to write commit messages.

## SILO Compatibility

This table shows which LAPIS version is required for which SILO version:

| LAPIS | SILO  |
|-------|-------|
| 0.2.1 | 0.2.0 |
| 0.1   | 0.1.0 |
