#!/bin/sh
# Takes JVM_OPTS as environment variable and passes it to the JVM
JVM_OPTS=${JVM_OPTS:-}
ARGS="${*}"

GENERAL_OPTS="-jar app.jar \
    --spring.profiles.active=docker \
    --referenceGenomeFilename=./reference_genomes.json \
    $ARGS"

if [ -n "$JVM_OPTS" ]; then
    CMD="java $JVM_OPTS $GENERAL_OPTS"
else
    CMD="java $GENERAL_OPTS"
fi
echo Running application with command:
echo "$CMD"
$CMD
