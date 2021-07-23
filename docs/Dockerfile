FROM slatedocs/slate as builder

WORKDIR /srv/slate
COPY source source
RUN /srv/slate/slate.sh build



FROM nginx:stable as server

COPY --from=builder /srv/slate/build /app
COPY docker_resources/nginx-docs.conf /etc/nginx/conf.d/default.conf

EXPOSE 4567
