FROM        ubuntu:16.04

MAINTAINER Maarten Pieck <maarten.pieck1@gmail.com>

#install weka
RUN         echo "deb http://us.archive.ubuntu.com/ubuntu vivid main universe" >> /etc/apt/sources.list 
RUN         apt-get update && apt-get install -y weka

ADD . /app
RUN         mkdir config && mv /app/weka-service.ini /config/
CMD         ["/bin/bash"]
