#!/bin/sh
# Takes JVM_OPTS as an environment variable and passes it to the JVM
JVM_OPTS=${JVM_OPTS:-}
ARGS="${*}"

POST_OPTS="-jar app.jar --spring.profiles.active=docker $ARGS"

if [ -n "$JVM_OPTS" ]; then
    CMD="java $JVM_OPTS $POST_OPTS"
else
    CMD="java $POST_OPTS"
fi
echo Running application with command:
echo "$CMD"
$CMD
