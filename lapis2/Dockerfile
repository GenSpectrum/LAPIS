FROM gradle:jdk21 AS build

WORKDIR /workspace

COPY . ./

RUN ./gradlew bootJar

FROM eclipse-temurin:21-alpine

WORKDIR /workspace

COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT [ \
    "java", \
    "-jar", \
    "app.jar", \
    "--referenceGenomeFilename=./reference_genomes.json", \
    "--spring.profiles.active=docker" \
    ]

LABEL org.opencontainers.image.source="https://github.com/GenSpectrum/LAPIS"
LABEL org.opencontainers.image.description="Lightweight API for Sequences: An API, a query engine, and a database schema for genomic sequences"
