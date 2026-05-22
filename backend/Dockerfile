FROM gradle:8-jdk17 AS build
WORKDIR /workspace
COPY settings.gradle ./
COPY openapi.yml ./
COPY backend ./backend
RUN gradle --no-daemon :backend:bootJar

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/backend/build/libs/*.jar /app/app.jar
COPY openapi.yml /app/openapi.yml
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
