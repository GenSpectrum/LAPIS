# LAPIS

This directory contains the LAPIS code.
LAPIS is a REST API written in Kotlin using Spring Boot.

## Running the Docker image

Check the [Docker compose file](docker-compose.yml) for an example on how to run the LAPIS Docker images.

Use Docker Compose to run SILO and LAPIS:

```bash
LAPIS_TAG=latest SILO_TAG=latest DATABASE_CONFIG=path/to/config docker compose up --pull always
```

### Running your local LAPIS changes

To test local changes to LAPIS, build the Docker image and run it with the compose file:

```bash
docker build --platform linux/amd64 -t ghcr.io/genspectrum/lapis:local .
SILO_TAG=latest LAPIS_TAG=local docker compose up --pull missing
```

## Configuration

When running LAPIS, you need to pass the following arguments:

* the SILO url `--silo.url=http://<url>:<port>`
* the path to the views config `--lapis.viewsConfig.path=<path/to/views.yaml>`,
  in the Docker image this is already set to `/workspace/views.yaml`.

The views config defines one or more URL-prefixed LAPIS views. Each view references a database config and a reference genome file relative to the views config file:

```yaml
views:
  - viewName: all
    baseQuery: default
    databaseConfig: database_config.yaml
    referenceGenome: reference_genomes.json
    capabilities:
      - metadata
      - mutations
      - insertions
      - sequences
      - phyloTree
      - components
```

With this example, LAPIS endpoints are available below `/all`, such as `/all/sample/aggregated`.

`baseQuery` is the relation used by metadata and sequence endpoints. LAPIS appends the request filter to this relation by default. A view that renames or projects fields can place `__LAPIS_REQUEST_FILTER__` exactly once to control where the request filter is applied and use `fieldAliases` to translate public field names in that filter:

```yaml
views:
  - viewName: swiss
    baseQuery: >-
      default.filter(country = 'Switzerland').filter(__LAPIS_REQUEST_FILTER__)
      .map({canton := division}).project({"strain", "canton", "main", "unaligned_main", "S"})
    tableScanQuery: default.filter(country = 'Switzerland').filter(__LAPIS_REQUEST_FILTER__)
    fieldAliases:
      canton: division
    databaseConfig: database_config_swiss.yaml
    referenceGenome: reference_genomes.json
    capabilities:
      - metadata
      - mutations
      - sequences
```

`tableScanQuery` is optional and is used by SILO operations that require a table scan, including mutations, insertions, and phylogenetic tree operations. Its schema must contain every configured metadata field, resolving names through `fieldAliases`. A projected `baseQuery` must include the sequence columns required by the configured reference genome when the `sequences` capability is enabled.

Optionally, you can pass:
* `lapis.docs.url` to make the "Documentation" link on the landing page (`/`) point to your self-hosted [lapis docs](../lapis-docs/README.md).
  If `lapis.docs.url` is not set or empty, then the "Documentation" link will not be shown.

Additionally, Apache Arrow requires these flags to be set on the JVM:
```
-Dio.netty.tryReflectionSetAccessible=true --add-opens=java.base/java.nio=ALL-UNNAMED
```
(see [Arrow docs](https://github.com/apache/arrow-java/?tab=readme-ov-file#java-properties)).
We already set the flags in Docker and when starting LAPIS via the Gradle `bootRun` command.

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
./gradlew bootRun --args='--silo.url=http://<url>:<port> --lapis.viewsConfig.path=<path/to/views.yaml>'
```
For example:
```
./gradlew bootRun --args='--silo.url=http://localhost:8091 --lapis.viewsConfig.path=../lapis-e2e/testData/singleSegmented/views.yaml --server.port=8090'
```

bootRun rebuilds the code as needed - if you want to ensure a fresh build you can first explicitly build lapis
```
./gradlew clean build
```

## ANTLR grammar


LAPIS uses [ANTLR](https://www.antlr.org/) as a parser generator for variant and advanced queries. The grammar can be found in `src/main/antlr`. When the package is built you can find the produced query parser, lexer and listener modules in `lapis/build/generated-src/antlr/...`.

To test the ANTLR parser you can run ANTLR locally on your grammar e.g. on the AdvancedQuery.g4 grammar:

```
antlr4 AdvancedQuery.g4 -o antlr-gen -visitor 
cd antlr-gen    

javac *.java
javac -cp ".:/path/to/antlr-4.13.2-complete.jar" *.java
// see the tokens - paste expression and then hit Crtl+D
java -cp ".:/path/to/antlr-4.13.2-complete.jar" org.antlr.v4.gui.TestRig AdvancedQuery start -tokens
// see the semantic tree - paste expression and then hit Crtl+D
java -cp ".:/path/to/antlr-4.13.2-complete.jar" org.antlr.v4.gui.TestRig AdvancedQuery start -tree
```

## OpenApi Docs

To generate the OpenApi docs run
```bash
./gradlew generateOpenApiDocs
```

To generate the OpenApi docs for an instance with multi-segmented reference genome run
```bash
./gradlew generateOpenApiDocs -Psegmented=true
```
