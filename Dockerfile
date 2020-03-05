FROM ubuntu:focal

RUN apt-get update \
 && apt-get install -y \
    openjdk-11-jre-headless \
 && apt-get clean

EXPOSE 8000

COPY target/*-standalone.jar pullq.jar
COPY pullq.conf pullq.conf

CMD ["java", "-jar", "pullq.jar"]
