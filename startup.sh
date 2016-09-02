#!/bin/bash
if [ -f /config/ClassifierParser.class]
then
	mv -f /config/ClassifierParser.java /app/src/main/java/Classifier/ClassifierParser.java
fi
cd /app
mvn clean compile assembly:single
cd /app/target
java -cp weka-service-1.0-SNAPSHOT-jar-with-dependencies.jar Main.Main
