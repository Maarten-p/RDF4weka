FROM        ubuntu:16.04

MAINTAINER Maarten Pieck <maarten.pieck1@gmail.com>

#install weka
RUN         echo "deb http://us.archive.ubuntu.com/ubuntu vivid main universe" >> /etc/apt/sources.list 
RUN         apt-get update && apt-get install -y openjdk-8-jdk weka maven

ADD . /app
RUN         mkdir config && mv /app/weka-service.ini /config/
ENV DATA_QUERY="prefix skosxl: <http://www.w3.org/2008/05/skos-xl#> \ 
		    prefix esco: <http://data.europa.eu/esco/model#> \
                    prefix mu: <http://mu.semte.ch/vocabularies/core/> \
                    select group_concat(distinct ?skillUuid; separator=\",\") as ?skillUuid  where { \
                    graph <http://localhost:8890/DAV> { \
                        ?s a esco:Occupation. \
                        ?relation esco:isRelationshipFor ?s. \
                        ?relation esco:refersConcept ?skill. \
                        ?s mu:uuid ?uuid. \
                        ?s skosxl:prefLabel / skosxl:literalForm ?label. \
                        ?skill skosxl:prefLabel / skosxl:literalForm ?skilllabel. \
                        ?skill mu:uuid ?skillUuid. \
                        FILTER ( lang(?label) = \"en\" ) \
                        FILTER ( lang(?skilllabel) = \"en\" ) \
                      } \
                    } \
                    group by ?uuid " ATTRIBUTES_QUERY="prefix skosxl: <http://www.w3.org/2008/05/skos-xl#> \
                prefix esco: <http://data.europa.eu/esco/model#> \
                prefix mu: <http://mu.semte.ch/vocabularies/core/> \
                select DISTINCT ?skillUuid  where { \
                  graph <http://localhost:8890/DAV> { \
                    ?s a esco:Occupation. \
                    ?relation esco:isRelationshipFor ?s. \
                    ?relation esco:refersConcept ?skill. \
                    ?skill skosxl:prefLabel / skosxl:literalForm ?skilllabel. \
                    ?skill mu:uuid ?skillUuid. \
                    FILTER ( lang(?skilllabel) = \"en\" ) \
                  } \
                }"

VOLUME /data
WORKDIR /data

EXPOSE 80

CMD         ["/bin/bash", "/app/startup.sh"]
