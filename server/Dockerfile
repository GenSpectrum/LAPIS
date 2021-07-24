## Build app ##

FROM openjdk:15 AS builder
WORKDIR /build/

COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test


## Run server ##

FROM openjdk:15 AS server
WORKDIR /app

RUN curl -fsSL "https://github.com/nextstrain/nextclade/releases/latest/download/nextalign-Linux-x86_64" -o "nextalign"
RUN chmod +x nextalign
COPY --from=builder /build/build/libs/lapis-1.0-SNAPSHOT.jar /app/lapis.jar

EXPOSE 2345
ENTRYPOINT ["java", "-Xmx7g", "-jar", "/app/lapis.jar", "--config", "/app/lapis-config.yml", "Lapis", "--api"]
