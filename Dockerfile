## Build time configuraton
FROM maven:3.8-eclipse-temurin-21-alpine AS maven_build
ENV HOME=/iri
RUN mkdir -p $HOME
WORKDIR $HOME
ADD . $HOME
RUN mvn -f $HOME/pom.xml clean install -Ddocker.nocache

FROM eclipse-temurin:21-jdk

ENV HOME=/iri
ENV LOGBACK="file:$HOME/config/logback.xml"
ENV CONFIG="file:$HOME/config/application.yaml"
ENV DEBUG_OPTS=""
ENV SSL_OPTS=""

USER 1000:1000
WORKDIR $HOME

COPY --from=MAVEN_BUILD $HOME/target/iri-facility-api-java-1.0.0.jar ./app.jar
COPY --from=MAVEN_BUILD $HOME/compose/config ./

EXPOSE 8081/tcp

CMD [ "sh", "-c", "\
  java \
    -Xmx1024m \
    -Djava.net.preferIPv4Stack=true \
    -Dlogging.config=$LOGBACK \
    $SSL_OPTS \
    $DEBUG_OPTS \
    -XX:+StartAttachListener \
    -jar $HOME/app.jar \
    --spring.config.location=$CONFIG" ]
