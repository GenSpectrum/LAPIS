# Getting started

## Build

The code for the software is located in the `server/` directory. It is a Java Gradle project. Java IDEs are usually capable to read the gradle configs and allow starting the program directly from the UI. To build the project in the terminal, run:

```
cd server/
./gradlew clean bootJar -x test
```

The .jar is stored in `build/libs`.

To run the unit tests, call:

```
./gradlew test
```


## API

This section describes how to start the API on a developer's local machine.

After building it as described above, the program has to be executed with the following arguments:

```
--config <path-to-config-file> Lapis --api
```

where the config file is a YAML file with the following content:

```
default:
  vineyard:
    host: <db host>
    port: <db port>
    dbname: <db name>
    username: <db user name>
    password: <db user password>
    schema: <db schema>
  apiOpennessLevel: <"OPEN" or "GISAID">
  cacheEnabled: false
  redisHost:
  redisPort:
```
