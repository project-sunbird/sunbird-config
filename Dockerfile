FROM openjdk:8-jre-alpine
MAINTAINER "S M Y" "smy.altamash@gmail.com"
RUN apk update \
    && apk add  unzip \
    && apk add curl \
    && adduser -u 1001 -h /home/sunbird/ -D sunbird \
    && mkdir -p /home/sunbird/config

