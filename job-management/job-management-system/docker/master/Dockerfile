FROM openjdk:21-jdk-slim

COPY master-server-jar-with-dependencies.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]