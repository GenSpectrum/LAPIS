## Download Swagger ##

FROM debian:bullseye AS builder
WORKDIR /app/

RUN apt-get update
RUN apt-get install -y curl
RUN curl -L -o swagger-ui.tar.gz https://github.com/swagger-api/swagger-ui/archive/refs/tags/v4.4.0.tar.gz
RUN tar -xf swagger-ui.tar.gz

## Run server ##

FROM nginx:stable AS server
WORKDIR /app/

COPY --from=builder /app/swagger-ui-4.4.0/dist/ /app/
COPY nginx-swagger.conf /etc/nginx/conf.d/default.conf
COPY v1.yml v1.yml
RUN sed -i 's/https:\/\/petstore.swagger.io\/v2\/swagger.json/v1.yml/g' index.html

EXPOSE 4568
