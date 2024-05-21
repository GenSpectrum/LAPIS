# LAPIS v2

This is a generalized Lightweight API for Sequences. It uses [SILO](https://github.com/GenSpectrum/LAPIS-SILO) as database for storing and querying the sequence data.

## OpenAPI documentation

The swagger ui is available at `url.to.lapis:<port>/swagger-ui.html`.

The OpenAPI specification is available at `url.to.lapis:<port>/api-docs` (in JSON format) or at 
`url.to.lapis:<port>/api-docs.yaml` (in YAML format).

## Local Setup

Run tests:
```
./gradlew test
```

Build Docker image:
```
./gradlew bootBuildImage
```

When running LAPIS, you need to pass the following arguments:
* the SILO url `--silo.url=http://<url>:<port>`, 
* the path to the database config `--lapis.databaseConfig.path=<path/to/config>`,
e.g. when running via gradle:
```
./gradlew bootRun --args='--silo.url=http://<url>:<port> --lapis.databaseConfig.path=<path/to/config> --referenceGenomeFilename=<path/to/referenceGenome>
```

## Running the Docker image

Check the [Docker compose file](docker-compose.yml) for an example on how to run the LAPIS Docker images.

Use Docker Compose to run SILO and LAPIS:
```
LAPIS_TAG=latest SILO_TAG=latest DATABASE_CONFIG=path/to/config docker compose up
```

### Operating LAPIS behind a proxy

When running LAPIS behind a proxy, the proxy needs to set X-Forwarded headers:
* X-Forwarded-For
* X-Forwarded-Proto
* X-Forwarded-Prefix

## End-to-end tests

There are end-to-end tests in `siloLapisTests/` that test the integration of SILO and LAPIS.

How to execute the tests
(Given that you have a running LAPIS instance listening on localhost:8090, e.g. via `docker compose up`):

* Generate the OpenAPI docs for LAPIS: `cd lapis2 && ./gradlew generateOpenApiDocs`
* Switch to test directory: `cd ../siloLapisTests/`
* Install NPM dependencies: `npm install`
* Generate a Typescript client for LAPIS: `npm run generateLapisClient`
* Execute the tests: `npm run test`

## Logs

LAPIS logs to rotating files in `./logs` and to stdout.
In the Docker container, log files are stored in `/workspace/logs`

## Creating A Release

This project uses [Release Please](https://github.com/google-github-actions/release-please-action) to generate releases.
On every commit on the `main` branch, it will update a Pull Request with a changelog.
When the PR is merged, the release will be created.
Creating a release means:

* A new Git tag is created.
* The Docker images of lapis2 and lapis2-docs are tagged with the new version.
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
