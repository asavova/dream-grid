FROM eclipse-temurin:17.0.13_11-jdk-jammy

WORKDIR /workspace

COPY . .

RUN apt-get update \
    && apt-get install -y --no-install-recommends git python3 python3-pip \
    && rm -rf /var/lib/apt/lists/*

RUN chmod +x gradlew \
    && ./gradlew --no-daemon clean test build

RUN python3 -m pip install --no-cache-dir -r requirements.txt -r python/requirements.txt \
    && python3 -m unittest discover -s python/tests

ENV DREAMGRID_SERVER_HOST=0.0.0.0
ENV DREAMGRID_SERVER_PORT=8080
ENV DREAMGRID_ANALYSIS_BASE_URL=http://python-analysis:5005
ENV DREAMGRID_DATABASE_PATH=/workspace/data/dreams.db

EXPOSE 8080
CMD ["java", "-jar", "/workspace/build/libs/dream-grid-1.0.jar"]
