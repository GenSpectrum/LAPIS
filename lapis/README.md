## Local Setup

Run tests:

```
./gradlew test
```

When running LAPIS, you need to pass the following arguments:

* the SILO url `--silo.url=http://<url>:<port>`,
* the path to the database config `--lapis.databaseConfig.path=<path/to/config>`,
* the path to the reference genome `--referenceGenomeFilename=<path/to/referenceGenome>`
  
e.g. when running via gradle:

```
./gradlew bootRun --args='--silo.url=http://<url>:<port> --lapis.databaseConfig.path=<path/to/config> --referenceGenomeFilename=<path/to/referenceGenome>
```

Optionally, you can pass:
* `lapis.docs.url` to make the "Documentation" link on the landing page (`/`) point to your self-hosted [lapis docs](../lapis-docs/README.md).
  If `lapis.docs.url` is not set or empty, then the "Documentation" link will not be shown.

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


## Logs

LAPIS logs to rotating files in `./logs` and to stdout.
In the Docker container, log files are stored in `/workspace/logs`
