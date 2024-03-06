FROM gradle:jdk21 AS build

WORKDIR /workspace

COPY . ./

RUN ./gradlew bootJar

FROM openjdk:21-slim

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
