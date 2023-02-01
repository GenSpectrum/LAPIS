#!/usr/bin/env bash

./gradlew clean bootJar -x test
java -jar build/libs/lapis-1.0-SNAPSHOT.jar --config config.yml --api
