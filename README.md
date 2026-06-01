# DreamGrid

DreamGrid is a personal dream journaling and analysis backend built with Java and Python.

The project stores dream entries, tracks analysis history, extracts and manages dream tags, and provides tools for exploring recurring themes and patterns over time. It combines deterministic rule-based analysis with optional AI-assisted analysis providers.

The goal of the project is to explore how dream data can be organized, classified, searched, and analyzed through a maintainable backend architecture.

## Features

- Dream storage and retrieval
- Dream analysis and reanalysis workflows
- Analysis history tracking
- Dynamic tag management
- Dream classification
- Pattern and tag insights
- Question and answer history
- Content safety validation
- Rule-based analysis engine
- SQLite persistence
- REST API

## Architecture

The application is split into two components.

### Java Backend

Responsible for:

- REST API endpoints
- Business logic
- Dream lifecycle management
- Classification workflows
- Persistence
- Insight generation

### Python Analysis Service

Responsible for:

- Rule-based dream analysis
- Tag extraction
- Theme detection
- Content safety checks
- Optional model-backed analysis

The Java backend communicates with the Python service through `DreamAnalysisClient`.

## Core Concepts

### Dream Entries

A dream entry represents a user-submitted dream and its associated metadata.

### Analysis Runs

Dreams may be analyzed multiple times. Analysis runs are stored separately to preserve historical results.

### Tags

Tags represent recurring symbols, themes, or concepts detected in dreams. Tags may be added manually or generated during analysis.

### Classification

Dreams may be classified into categories such as lucid, nightmare, recurring, or neutral. Classifications may be user-provided or inferred from analysis and historical patterns.

### Insights

The system can generate deterministic insights based on stored dream data, including recurring tags, tag frequency, and tag relationships.

## Running the Project

### Java Backend

```bash
./gradlew run
```

Windows:

```powershell
.\gradlew.bat run
```

### Python Service

```bash
pip install -r python/requirements.txt
python python/analysis_api.py
```

### Tests

```bash
./gradlew test
```

Windows:

```powershell
.\gradlew.bat test
```

### Build

```bash
./gradlew build
```

Windows:

```powershell
.\gradlew.bat build
```

### Docker

```bash
docker build -t dreamgrid .
docker compose up --build
```

## Repository Structure

```text
src/            Java application
python/         Python analysis service
gradle/         Gradle wrapper files
build.gradle    Gradle build configuration
API.md          API reference
ARCHITECTURE.md Architecture notes
```

## Project Status

DreamGrid is an active personal project focused on backend architecture, analysis workflows, classification logic, and long-term dream pattern tracking.
