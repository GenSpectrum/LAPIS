## Build doc ##

FROM ruby:3-buster AS builder

RUN apt-get update
RUN apt-get install -y nodejs

WORKDIR /app
RUN gem install bundler
RUN git clone https://github.com/slatedocs/slate.git

WORKDIR /app/slate
RUN bundle install
COPY source source
RUN bundle exec middleman build


## Run server ##

FROM nginx:stable AS server

COPY --from=builder /app/slate/build /app/
COPY docker_resources/nginx-docs.conf /etc/nginx/conf.d/default.conf

EXPOSE 4567
