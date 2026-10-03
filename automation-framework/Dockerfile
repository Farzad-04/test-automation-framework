FROM node:22-alpine AS frontend
WORKDIR /workspace
COPY frontend ./frontend
WORKDIR /workspace/frontend
RUN npm ci && npm run build

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
COPY testng.xml .
COPY --from=frontend /workspace/src/main/resources/static ./src/main/resources/static
RUN mvn --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system banking && useradd --system --gid banking banking
COPY --from=build /workspace/target/automation-framework-1.0-SNAPSHOT.jar app.jar
USER banking
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
