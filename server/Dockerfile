## Build app ##

FROM openjdk:16 AS builder
WORKDIR /build/

COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test


## Run server ##

FROM openjdk:16-bullseye AS server
WORKDIR /app

# Install SSH and corkscrew
# https://github.com/bryanpkc/corkscrew
RUN apt-get update
RUN apt-get install -y openssh-client git
RUN git clone https://github.com/bryanpkc/corkscrew.git
WORKDIR /app/corkscrew
RUN apt-get install -y build-essential autoconf
RUN autoreconf --install
RUN ./configure
RUN make
RUN make install
WORKDIR /app

# Download nextalign
RUN curl -fsSL "https://github.com/nextstrain/nextclade/releases/latest/download/nextalign-Linux-x86_64" -o "nextalign"
RUN chmod +x nextalign

# Download geo location corrections list from Nextstrain
RUN curl https://raw.githubusercontent.com/nextstrain/ncov-ingest/master/source-data/gisaid_geoLocationRules.tsv -o gisaid_geoLocationRules.tsv

COPY --from=builder /build/build/libs/lapis-1.0-SNAPSHOT.jar /app/lapis.jar

EXPOSE 2345
ENTRYPOINT ["java", "-Xmx7g", "-jar", "/app/lapis.jar", "--config", "/app/lapis-config.yml", "Lapis", "--api"]
