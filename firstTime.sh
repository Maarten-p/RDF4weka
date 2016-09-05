#!/bin/bash
if [ -f "/config/ClassifierParser.class" ] && [ cmp "/config/ClassifierParser.class" "/app/src/main/java/Classifier/classifierParser.java" ];
then
	mv -f /config/ClassifierParser.java /app/src/main/java/Classifier/ClassifierParser.java
	cd /app
	mvn clean compile assembly:single
	cd /
elif [ ! -f "/app/target/weka-service-1.0-SNAPSHOT-jar-with-dependencies.jar" ];
then
	cd /app
	mvn clean compile assembly:single
	cd /
fi