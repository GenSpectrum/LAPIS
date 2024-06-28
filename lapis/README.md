## Local Setup

Run tests:

```bash
./gradlew test
```

When running LAPIS, you need to pass the following arguments:

* the SILO url `--silo.url=http://<url>:<port>`,
* the path to the database config `--lapis.databaseConfig.path=<path/to/config>`,
  e.g. when running via gradle:

```bash
./gradlew bootRun --args='--silo.url=http://<url>:<port> --lapis.databaseConfig.path=<path/to/config> --referenceGenomeFilename=<path/to/referenceGenome>
```

## Running the Docker image

Check the [Docker compose file](docker-compose.yml) for an example on how to run the LAPIS Docker images.

Use Docker Compose to run SILO and LAPIS:

```bash
LAPIS_TAG=latest SILO_TAG=latest DATABASE_CONFIG=path/to/config docker compose up
```

### Operating LAPIS behind a proxy

When running LAPIS behind a proxy, the proxy needs to set X-Forwarded headers:

* X-Forwarded-For
* X-Forwarded-Proto
* X-Forwarded-Prefix

## Logs

LAPIS logs to rotating files in `./logs` and to stdout.
In the Docker container, log files are stored in `/workspace/logs`

## Cache

LAPIS uses an in-memory cache to store the results of the last queries. The default size is 50000 entries or 500MB,
which ever is reached faster. If the cache is full, the least recently used entry is removed from the cache.
The cache is cleared when the server is restarted, or SILO provides a new data version.
The size of the cache can be configured in the `application.properties` file

```
spring.cache.caffeine.spec=maximumSize=50000
spring.cache.caffeine.maxSizeMb=500
```

or by providing command line arguments to
the execution:

```bash
--spring.cache.caffeine.spec=maximumSize=50000 --spring.cache.caffeine.maxSizeMb=500
```
