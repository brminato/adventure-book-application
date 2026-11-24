FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /src
COPY pom.xml .
COPY src src
RUN mvn -B verify

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN mkdir -p /app/data/books && chown -R 10001:10001 /app/data
COPY --from=build /src/target/adventure-book.jar app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
