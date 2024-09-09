FROM gradle:jdk21 AS build

WORKDIR /workspace

COPY . ./

RUN ./gradlew bootJar

FROM eclipse-temurin:21-alpine

WORKDIR /workspace

COPY --from=build /workspace/build/libs/*.jar app.jar

COPY entrypoint.sh entrypoint.sh
RUN chmod +x entrypoint.sh

EXPOSE 8080

HEALTHCHECK --interval=30s --start-period=30s --start-interval=5s --timeout=3s --retries=3 CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["./entrypoint.sh"]

LABEL org.opencontainers.image.source="https://github.com/GenSpectrum/LAPIS/lapis"
LABEL org.opencontainers.image.description="Lightweight API for Sequences: An API, a query engine, and a database schema for genomic sequences"
