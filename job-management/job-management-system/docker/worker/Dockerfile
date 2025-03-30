FROM openjdk:21-jdk-slim

COPY worker-server-jar-with-dependencies.jar app.jar

CMD ["java", "-jar", "app.jar", "<worker_port>", "<worker_password>", "<initial_budget>"]