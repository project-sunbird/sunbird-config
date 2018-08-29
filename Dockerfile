FROM tomcat:8-jre8
MAINTAINER "S M Y" "smy.altamash@gmail.com"
RUN apt update \
    && useradd -ms /bin/bash sunbird \
    && mkdir -p /home/sunbird/config
WORKDIR /home/sunbird/config
ADD config-base/service/target/config-service.war /usr/local/tomcat/webapps/
