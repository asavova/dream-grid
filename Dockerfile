FROM eclipse-temurin:17.0.13_11-jdk-jammy@sha256:292214e32a0a3032e7f1af0e22491e02452c1f5473ec1d96486958dd4b3a772a

WORKDIR /workspace

COPY . .

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        git=1:2.34.1-1ubuntu1.17 \
        python3=3.10.6-1~22.04.1 \
        python3-pip=22.0.2+dfsg-1ubuntu0.7 \
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
