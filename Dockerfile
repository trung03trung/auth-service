# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

ARG GITHUB_USER
ARG GITHUB_TOKEN

COPY pom.xml .
COPY settings.xml /root/.m2/settings.xml
COPY src ./src

ENV GITHUB_USER=${GITHUB_USER}
ENV GITHUB_TOKEN=${GITHUB_TOKEN}

RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-Xmx256m", "-XX:+UseSerialGC", "-jar", "app.jar", "--spring.profiles.active=prod", "--server.port=${PORT:=8080}"]
