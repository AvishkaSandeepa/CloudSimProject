FROM openjdk:21-jdk-slim

COPY master-cli-jar-with-dependencies.jar app.jar

CMD ["java", "-jar", "app.jar", "<master_host>", "<master_port>"]