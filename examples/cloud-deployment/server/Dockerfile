FROM registry.access.redhat.com/ubi9/openjdk-17:1.20

ENV LANGUAGE='en_US:en'

# Labels for documentation and metadata
LABEL description="A2A Cloud Deployment Demo - Multi-pod agent with Kafka event replication"
LABEL version="1.0.0"

# Copy the Quarkus application (fast-jar layout)
COPY --chown=185 target/quarkus-app/lib/ /deployments/lib/
COPY --chown=185 target/quarkus-app/*.jar /deployments/
COPY --chown=185 target/quarkus-app/app/ /deployments/app/
COPY --chown=185 target/quarkus-app/quarkus/ /deployments/quarkus/

EXPOSE 8080
USER 185

# Java options for Quarkus
ENV JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

ENTRYPOINT [ "/opt/jboss/container/java/run/run-java.sh" ]
