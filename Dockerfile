FROM eclipse-temurin:17.0.13_11-jdk-jammy AS build
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew && ./gradlew --no-daemon clean test build

FROM eclipse-temurin:17.0.13_11-jre-jammy
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar /app/dream-grid.jar

ENV DREAMGRID_SERVER_HOST=0.0.0.0
ENV DREAMGRID_SERVER_PORT=8080
ENV DREAMGRID_ANALYSIS_BASE_URL=http://python-analysis:5005
ENV DREAMGRID_DATABASE_PATH=/app/data/dreams.db

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/dream-grid.jar"]
