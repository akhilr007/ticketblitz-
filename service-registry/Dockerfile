# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY ../pom.xml ../pom.xml
COPY ../common/pom.xml ../common/pom.xml
COPY pom.xml .
COPY src ./src
COPY ../mvnw ../mvnw
COPY ../.mvn ../.mvn
RUN cd .. && ./mvnw -pl service-registry -am package -DskipTests -q

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
