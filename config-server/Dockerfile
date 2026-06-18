FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY target/config-server-1.0-SNAPSHOT.jar app.jar

EXPOSE 8888

ENTRYPOINT ["java", "-jar", "app.jar"]
