FROM node:lts-alpine as build-deps

WORKDIR /app

RUN apk update && apk add curl

COPY . .
ENV CONFIG_FILE=/config/database_config.yaml
ENV REFERENCE_GENOMES_FILE=/config/reference_genomes.json

EXPOSE 3000
VOLUME /config

HEALTHCHECK --start-period=60s CMD curl --fail --silent localhost:3000$BASE_URL | grep "Welcome to LAPIS" || exit 1

CMD npm ci && npm run build && npm run preview -- --host 0.0.0.0 --port 3000
