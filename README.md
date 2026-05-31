# DreamGrid

DreamGrid is a local backend for storing dream entries and running AI-assisted analysis on them. The Java service owns the API, persistence, and workflow rules. A small Python service handles the text analysis model.

The project is intentionally split into plain layers so the core behavior is easy to follow: API handlers call a service, the service coordinates repositories and clients, repositories persist to SQLite, and the Python model stays behind an HTTP boundary.

## What It Does

- Stores dream entries in SQLite
- Extracts basic symbolic tags from dream text
- Sends dream content to a local Python analysis service
- Persists analysis results with status, timestamp, and version metadata
- Reuses cached analysis when the stored result is still valid
- Supports forced reanalysis without deleting the previous successful result on failure
- Exposes the workflow through a Java REST API

## Architecture

```text
Java API (:8080)
    |
    v
DreamApiHandler
    |
    v
DreamService
    |------------------------.
    v                        v
DreamRepository       DreamAnalysisClient
    |                        |
    v                        v
SQLite             Python analysis API (:5005)
```

The main Java packages are:

- `api` - HTTP routing and JSON responses
- `service` - application workflow and business rules
- `repository` - JDBC persistence
- `database` - SQLite connection and schema setup
- `client` - HTTP calls to the Python analysis service
- `model` - domain objects and enums
- `dto` - request and response payloads

The Python service is under `python/`:

- `analysis_api.py` - Flask routes and request validation
- `analysis_service.py` - model interaction, symbol detection, and theme detection
- `models/analysis_result.py` - typed analysis response model
- `config.py` - service and model configuration

## Repository Layout

```text
src/main/java/
  Main.java
  com/dreamgrid/
    api/
    client/
    database/
    dto/
    model/
    repository/
    service/

src/test/java/
  com/dreamgrid/service/

python/
  analysis_api.py
  analysis_service.py
  config.py
  models/
  tests/

requirements.txt
build.gradle
```

## Database

Dreams are stored in `data/dreams.db`. The Java application creates the database directory and the `dreams` table at startup. Schema updates are additive so existing records are not dropped.

Current table:

```sql
CREATE TABLE IF NOT EXISTS dreams (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    dream_date TEXT,
    timestamp INTEGER,
    symbol_tags TEXT,
    dream_type TEXT,
    analyzed INTEGER DEFAULT 0,
    analysis_result TEXT,
    analyzed_at INTEGER,
    analysis_version TEXT,
    analysis_status TEXT NOT NULL DEFAULT 'PENDING'
);
```

`analysis_status` is the canonical analysis state. `analyzed` remains as a compatibility flag derived from completed analysis state.

## API

The Java API runs on `http://localhost:8080`.

### Health

```http
GET /health
```

```json
{
  "status": "ok"
}
```

### Create a Dream

```http
POST /dreams
Content-Type: application/json
```

```json
{
  "title": "Mountain Climb",
  "content": "I was climbing a mountain under a clear sky with fire on the horizon",
  "date": "2026-05-31",
  "type": "VISION"
}
```

New dreams start with `analysisStatus` set to `PENDING`.

### Read Dreams

```http
GET /dreams
GET /dreams/{id}
```

### Analyze a Dream

```http
POST /dreams/{id}/analyze
```

If the dream already has a completed analysis for the current analysis version, the stored result is returned. Otherwise the Java service calls the Python analysis API and stores the result.

### Force Reanalysis

```http
POST /dreams/{id}/reanalyze
```

This always calls the Python analysis service. On success, the previous analysis is replaced. On failure, the old successful result is kept and the status is set to `FAILED`.

## Python Analysis Service

The Python service exposes:

```http
GET /health
POST /analyze
```

`POST /analyze` accepts:

```json
{
  "dream": "I walked through a portal under a bright sky."
}
```

It returns structured analysis data:

```json
{
  "summary": "A concise interpretation of the dream.",
  "detectedSymbols": ["PORTAL", "SKY"],
  "detectedThemes": ["freedom", "transition"],
  "confidenceScore": 0.85,
  "modelVersion": "v1"
}
```

The Java service stores the Python response as the dream analysis result.

## Running Locally

Requirements:

- Java 17+
- Gradle, or a complete Gradle wrapper checkout
- Python 3
- Python dependencies from `requirements.txt`

Install Python dependencies:

```bash
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
```

On macOS/Linux, activate the environment with:

```bash
source .venv/bin/activate
```

Start the Python service first:

```bash
python python/analysis_api.py
```

Then start the Java API:

```bash
gradle run
```

If the Gradle wrapper JAR is present in your checkout:

```bash
./gradlew run
```

On Windows:

```bash
gradlew.bat run
```

## Testing

Java tests:

```bash
gradle test
```

Python tests:

```bash
python -m unittest discover -s python/tests
```

The current Java tests cover analysis caching, forced reanalysis, failed reanalysis preservation, stale analysis versions, and missing dream handling. The Python tests cover the analysis service model output and Flask API behavior when Flask is installed.

## Notes

- The Java API expects the Python analysis service on `http://127.0.0.1:5005/analyze`.
- The default Python model is configured in `python/config.py`.
- SQLite data is local and stored under `data/`.

## Roadmap

- Search over stored dreams
- Dream similarity scoring
- Symbol extraction history
- Multiple stored analysis versions per dream
- Authentication
- Docker support
- More integration-level API tests
