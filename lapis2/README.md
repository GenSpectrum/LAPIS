# LAPIS v2

This is a generalized Lightweight API for Sequences. It uses [SILO](https://github.com/GenSpectrum/LAPIS-SILO) as database for storing and querying the sequence data.

## Setup

Run tests:
```
./gradlew test
```

Build Docker image:
```
./gradlew bootBuildImage
```

When running LAPIS, you need to pass the SILO url as argument: `--silo.url=http://<url>:<port>`, e.g. when running via gradle:
```
./gradlew bootRun --args='--silo.url=http://<url>:<port>'
```

Use Docker Compose to run SILO and LAPIS:
```
LAPIS_TAG=latest SILO_TAG=latest docker compose up
```
