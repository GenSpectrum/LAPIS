#!/bin/sh
# Takes JVM_OPTS as environment variable and passes it to the JVM
JVM_OPTS=${JVM_OPTS:-}
ARGS="${*}"

# Required by Apache Arrow (Netty memory backend) to access DirectByteBuffer on Java 9+
# See https://github.com/apache/arrow-java/?tab=readme-ov-file#java-properties
ARROW_OPTS="-Dio.netty.tryReflectionSetAccessible=true --add-opens=java.base/java.nio=ALL-UNNAMED"

# Terminate the JVM on OutOfMemoryError instead of leaving it in a degraded, GC-thrashing
# state, so that the orchestrator restarts the container. Override via JVM_OPTS if needed.
DEFAULT_JVM_OPTS="-XX:+ExitOnOutOfMemoryError"

GENERAL_OPTS="$ARROW_OPTS -jar app.jar \
    --spring.profiles.active=docker \
    --referenceGenomeFilename=./reference_genomes.json \
    $ARGS"

CMD="java $DEFAULT_JVM_OPTS $JVM_OPTS $GENERAL_OPTS"
echo Running application with command:
echo "$CMD"
$CMD
