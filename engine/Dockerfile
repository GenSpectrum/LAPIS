# -------------
FROM rust:1-buster as builder
WORKDIR /app

COPY . .
RUN cargo build --release

# -------------
FROM debian:buster-slim as runtime

COPY --from=builder /app/target/release/lapis_v2_engine /usr/local/bin/lapis-engine

CMD ["lapis-engine"]
