FROM maven:3.9-eclipse-temurin-21 AS build
ARG GITHUB_USER
ARG GITHUB_TOKEN
WORKDIR /app
COPY pom.xml .
COPY settings.xml .
RUN mvn dependency:go-offline -s settings.xml -q || true
COPY src src
RUN mvn clean package -s settings.xml -DskipTests -q

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=dev"]
