# DreamGrid

DreamGrid is a local backend for storing dreams and running AI-assisted analysis on them. The Java service handles the REST API, SQLite persistence, and workflow rules. The Python service handles model prompts and returns structured analysis data.

## Current Features

- Create and read dream entries
- Persist dreams in SQLite
- Analyze dreams through a local Python service
- Store analysis result, status, timestamp, and version
- Reuse cached analysis when it is still valid
- Force a new analysis when needed
- Ask follow-up questions about an analyzed dream

## Architecture

```text
Java API (:8080)
    |
    v
DreamService
    |----------------------.
    v                      v
DreamRepository     DreamAnalysisClient
    |                      |
    v                      v
SQLite          Python analysis service (:5005)
```

Main Java packages:

- `api` - HTTP handlers
- `service` - application workflow
- `repository` - SQLite reads and writes
- `database` - connection and schema setup
- `client` - HTTP calls to the Python service
- `model` - domain objects and enums
- `dto` - request and response payloads

Python files live under `python/`:

- `analysis_api.py` - Flask routes
- `analysis_service.py` - model prompts, response parsing, and question answering
- `models/analysis_result.py` - structured analysis result
- `config.py` - model and service settings

## Repository Layout

```text
src/main/java/com/dreamgrid/
  api/
  client/
  database/
  dto/
  model/
  repository/
  service/

src/test/java/com/dreamgrid/service/

python/
  analysis_api.py
  analysis_service.py
  config.py
  models/
  tests/
```

## Database

DreamGrid stores data in `data/dreams.db`. The database is created automatically when the Java app starts.

The main table is `dreams`:

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

`analysis_status` is the main state field. `analyzed` is kept as a compatibility flag.

## Running Locally

Requirements:

- Java 17+
- Gradle wrapper included in the repository
- Python 3

Install Python dependencies:

```bash
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
```

On macOS/Linux:

```bash
source .venv/bin/activate
```

Start the Python service:

```bash
python python/analysis_api.py
```

Start the Java API:

```bash
gradlew.bat run
```

On macOS/Linux:

```bash
./gradlew run
```

## API

The Java API runs on `http://localhost:8080`.

Common endpoints:

```http
GET  /health
GET  /dreams
GET  /dreams/{id}
POST /dreams
POST /dreams/{id}/analyze
POST /dreams/{id}/reanalyze
POST /dreams/{id}/questions
```

See [API.md](API.md) for request and response examples.

## Testing

Java:

```bash
gradlew.bat test
gradlew.bat build
```

Python:

```bash
python -m unittest discover -s python/tests
```

## Notes

- The Java service expects the Python analysis API at `http://127.0.0.1:5005`.
- The Python model and version are configured in `python/config.py`.
- Analysis symbols and themes come from the model response. The Java backend only mirrors known symbols into its existing enum field.

## Roadmap

- Search over stored dreams
- Analysis history
- Similarity scoring
- Symbol statistics
- More integration tests
