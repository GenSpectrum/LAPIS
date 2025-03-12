# LAPIS

This directory contains the LAPIS code.
LAPIS is a REST API written in Kotlin using Spring Boot.

## Running the Docker image

Check the [Docker compose file](docker-compose.yml) for an example on how to run the LAPIS Docker images.

Use Docker Compose to run SILO and LAPIS:

```bash
LAPIS_TAG=latest SILO_TAG=latest DATABASE_CONFIG=path/to/config docker compose up
```

## Configuration

When running LAPIS, you need to pass the following arguments:

* the SILO url `--silo.url=http://<url>:<port>`
* the path to the database config `--lapis.databaseConfig.path=<path/to/config>`,
 in the Docker image this is already set to `/workspace/database_config.yaml`.
* the path to the reference genome `--referenceGenomeFilename=<path/to/referenceGenome>`
  in the Docker image this is already set to `/workspace/reference_genomes.yaml`.

Optionally, you can pass:
* `lapis.docs.url` to make the "Documentation" link on the landing page (`/`) point to your self-hosted [lapis docs](../lapis-docs/README.md).
  If `lapis.docs.url` is not set or empty, then the "Documentation" link will not be shown.

### Operating LAPIS behind a proxy

When running LAPIS behind a proxy, the proxy needs to set X-Forwarded headers:

* X-Forwarded-For
* X-Forwarded-Proto
* X-Forwarded-Prefix

## Logs

LAPIS logs to rotating files in `./logs` and to stdout.
In the Docker container, log files are stored in `/workspace/logs`

## Cache

By default, LAPIS uses an in-memory cache to store the results of the last queries for the endpoints
aggregated, nucleotideMutation, aminoAcidMutation, nucleotideInsertions and aminoAcidInsertions.

The default cache provider is Caffeine, with soft references for the values and a maximum size of 50000 entries.
This configuration can be changed in the `application.properties` file
```
spring.cache.caffeine.spec=maximumSize=50000,softValues
```
or by providing command line arguments to the execution:
```bash
--spring.cache.caffeine.spec=maximumSize=50000,softValues
```

We use soft references to allow the garbage collector to remove entries from the cache if the memory is needed.
However, per default the cache is not guaranteed to have a fixed memory size, increasing with each stored entry. 
The maintainer must ensure that enough memory is available to store the cache entries, or provide a limit to the 
heap size of the JVM.

If the cache is full, the least recently used entry is removed from the cache.
The cache is cleared when the server is restarted, or SILO provides a new data version.

The cache can be turned off by providing the `spring.cache.type` attribute in the 
`application.properties` file, for example: 
```
spring.cache.type=none
```
or by providing the command line argument:
```bash
--spring.cache.type=none
```

## Local Setup

Run tests:

```bash
./gradlew test
```

e.g. when running via gradle:

```bash
./gradlew bootRun --args='--silo.url=http://<url>:<port> --lapis.databaseConfig.path=<path/to/config> --referenceGenomeFilename=<path/to/referenceGenome>'
```
For example:
```
./gradlew bootRun --args='--silo.url=http://localhost:8091 --lapis.databaseConfig.path=../lapis-e2e/testData/singleSegmented/testDatabaseConfig.yaml --referenceGenomeFilename=../lapis-e2e/testData/singleSegmented/reference_genomes.json  --server.port=8090'
```
