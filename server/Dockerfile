## Build app ##

FROM openjdk:16 AS builder
WORKDIR /build/

COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test


## Run server ##

FROM openjdk:16-bullseye AS server
WORKDIR /app

# Download nextalign
RUN curl -fsSL "https://github.com/nextstrain/nextclade/releases/download/2.3.1/nextalign-x86_64-unknown-linux-gnu" -o "nextalign"
RUN chmod +x nextalign

# Download Nextclade
RUN curl -fsSL "https://github.com/nextstrain/nextclade/releases/download/2.3.1/nextclade-x86_64-unknown-linux-gnu" -o "nextclade"
RUN chmod +x nextclade
RUN ./nextclade dataset get --name 'sars-cov-2' --output-dir 'nextclade-data'

# Download geo location corrections list from Nextstrain
RUN curl https://raw.githubusercontent.com/nextstrain/ncov-ingest/master/source-data/gisaid_geoLocationRules.tsv -o gisaid_geoLocationRules.tsv

COPY --from=builder /build/build/libs/lapis-1.0-SNAPSHOT.jar /app/lapis.jar

EXPOSE 2345
ENTRYPOINT ["java", "-Xmx10g", "-jar", "/app/lapis.jar", "--config", "/app/lapis-config.yml", "Lapis", "--api"]
