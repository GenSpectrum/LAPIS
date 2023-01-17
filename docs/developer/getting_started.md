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

After building it as described above, the program can be started with `server/startServer.sh`.
The script assumes that the .jar file is present and that `server/config.yml.example` is copied to `server/config.yml`.
