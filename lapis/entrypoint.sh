#!/bin/sh
# Takes JVM_OPTS as environment variable and passes it to the JVM
JVM_OPTS=${JVM_OPTS:-}
ARGS="${*}"

# Required by Apache Arrow (Netty memory backend) to access DirectByteBuffer on Java 9+
# See https://github.com/apache/arrow-java/?tab=readme-ov-file#java-properties
ARROW_OPTS="-Dio.netty.tryReflectionSetAccessible=true --add-opens=java.base/java.nio=ALL-UNNAMED"

GENERAL_OPTS="$ARROW_OPTS -jar app.jar \
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
