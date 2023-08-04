# LAPIS v2

This is a generalized Lightweight API for Sequences. It uses [SILO](https://github.com/GenSpectrum/LAPIS-SILO) as database for storing and querying the sequence data.

## OpenAPI documentation

The swagger ui is available at `url.to.lapis:<port>/swagger-ui.html`.

The OpenAPI specification is available at `url.to.lapis:<port>/api-docs` (in JSON format) or at 
`url.to.lapis:<port>/api-docs.yaml` (in YAML format).

## Setup

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
./gradlew bootRun --args='--silo.url=http://<url>:<port> --lapis.databaseConfig.path=<path/to/config>'
```

Use Docker Compose to preprocess data, e.g. via:
```
LAPIS_TAG=latest SILO_TAG=latest docker compose -f docker-compose-silo-preprocessing-for-e2e.yml up
```

Use Docker Compose to run SILO and LAPIS (after preprocessing):
```
LAPIS_TAG=latest SILO_TAG=latest docker compose up
```

## End-to-end tests

There are end-to-end tests in `siloLapisTests/` that test the integration of SILO and LAPIS.

How to execute the tests
(Given that you have a running LAPIS instance listening on localhost:8080, e.g. via `docker compose up`):

* Generate the OpenAPI docs for LAPIS: `cd lapis2 && ./gradlew generateOpenApiDocs`
* Switch to test directory: `cd ../siloLapisTests/`
* Install NPM dependencies: `npm install`
* Generate a Typescript client for LAPIS: `npm run generateLapisClient`
* Execute the tests: `npm run test`
