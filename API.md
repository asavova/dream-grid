# DreamGrid REST API Documentation

## Overview

DreamGrid is a production-style REST API backend for dream storage, analysis, and retrieval. It uses:

- **Java 17** with built-in HTTP server
- **SQLite** for persistent dream storage
- **GSON** for JSON serialization
- **Python API** for dream analysis (external service)

## Architecture

```
Main
  └─ DreamGridServer (HTTP Server on port 8080)
      └─ DreamApiHandler (API request routing)
          └─ DreamService (business logic)
              ├─ DreamRepository (SQLite persistence)
              └─ DreamAnalysisClient (Python API communication)
```

### Layered Structure

- **API Layer** (`com.dreamgrid.api`): HTTP request/response handling
- **Service Layer** (`com.dreamgrid.service`): Business logic and orchestration
- **Repository Layer** (`com.dreamgrid.repository`): Database persistence
- **Database Layer** (`com.dreamgrid.database`): Connection & schema management
- **Client Layer** (`com.dreamgrid.client`): External API communication
- **Model Layer** (`com.dreamgrid.model`): Domain entities
- **DTO Layer** (`com.dreamgrid.dto`): API request/response objects

## API Endpoints

### 1. Health Check

**Endpoint:** `GET /health`

**Response:**
```json
{
  "status": "ok"
}
```

**Status Code:** 200 OK

---

### 2. Create Dream

**Endpoint:** `POST /dreams`

**Request Body:**
```json
{
  "title": "A Vivid Journey",
  "content": "I was walking through a forest with a portal appearing...",
  "date": "2026-05-31",
  "type": "VISION"
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "title": "A Vivid Journey",
  "content": "I was walking through a forest with a portal appearing...",
  "dreamDate": "2026-05-31 14:22:45",
  "timestamp": 1748736165000,
  "symbolTags": ["PORTAL", "SKY"],
  "dreamType": "VISION",
  "analyzed": false,
  "analysisResult": null,
  "analyzedAt": null,
  "analysisVersion": null,
  "analysisStatus": "PENDING"
}
```

**Validation:**
- `title` cannot be empty
- `content` cannot be empty
- `type` must be valid DreamType enum (LUCID, NIGHTMARE, RECURRING, VISION, ORDINARY, NONE)
- `date` optional; auto-generated if not provided

**Status Codes:**
- 201 Created - Dream successfully created
- 400 Bad Request - Validation error
- 500 Internal Server Error - Database error

---

### 3. List All Dreams

**Endpoint:** `GET /dreams`

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "title": "A Vivid Journey",
    "content": "I was walking through a forest with a portal appearing...",
    "dreamDate": "2026-05-31 14:22:45",
    "timestamp": 1748736165000,
    "symbolTags": ["PORTAL", "SKY"],
    "dreamType": "VISION",
    "analyzed": true,
    "analysisResult": "This dream represents your inner journey through psychological barriers...",
    "analyzedAt": 1748736300000,
    "analysisVersion": "v1",
    "analysisStatus": "COMPLETED"
  },
  {
    "id": 2,
    "title": "Fiery Descent",
    "content": "The sky was burning with fire...",
    "dreamDate": "2026-05-31 14:25:10",
    "timestamp": 1748736310000,
    "symbolTags": ["FIRE", "SKY"],
    "dreamType": "NIGHTMARE",
    "analyzed": false,
    "analysisResult": null,
    "analyzedAt": null,
    "analysisVersion": null,
    "analysisStatus": "PENDING"
  }
]
```

**Status Codes:**
- 200 OK - Dreams retrieved successfully
- 500 Internal Server Error - Database error

---

### 4. Get Dream by ID

**Endpoint:** `GET /dreams/{id}`

**Path Parameters:**
- `id` (required): Dream ID (integer)

**Response (200 OK):**
```json
{
  "id": 1,
  "title": "A Vivid Journey",
  "content": "I was walking through a forest with a portal appearing...",
  "dreamDate": "2026-05-31 14:22:45",
  "timestamp": 1748736165000,
  "symbolTags": ["PORTAL", "SKY"],
  "dreamType": "VISION",
  "analyzed": true,
  "analysisResult": "This dream represents your inner journey through psychological barriers...",
  "analyzedAt": 1748736300000,
  "analysisVersion": "v1",
  "analysisStatus": "COMPLETED"
}
```

**Status Codes:**
- 200 OK - Dream found
- 404 Not Found - Dream with given ID does not exist
- 500 Internal Server Error - Database error

---

### 5. Analyze Dream

**Endpoint:** `POST /dreams/{id}/analyze`

**Path Parameters:**
- `id` (required): Dream ID (integer)

**Response (200 OK):**
```json
{
  "dreamId": 1,
  "analysis": "This dream represents your inner journey through psychological barriers...",
  "analyzedAt": 1748736300000,
  "analysisVersion": "v1",
  "analysisStatus": "COMPLETED"
}
```

**Process:**
1. Retrieves dream from database by ID
2. Returns the stored analysis when `analysisStatus` is `COMPLETED`, `analysisResult` is present,
   and `analysisVersion` matches the configured analyzer version
3. Otherwise sends dream content to the external Python analysis API
4. Stores the returned analysis result and marks the dream `COMPLETED`
5. Returns analysis result and analysis metadata

If the Python analysis API fails, the dream is marked `FAILED` while any previous successful
analysis text, timestamp, and version are preserved.

**Status Codes:**
- 200 OK - Analysis successful
- 404 Not Found - Dream with given ID does not exist
- 502 Bad Gateway - Analysis API error
- 500 Internal Server Error - Database error

---

### 6. Reanalyze Dream

**Endpoint:** `POST /dreams/{id}/reanalyze`

**Path Parameters:**
- `id` (required): Dream ID (integer)

**Response (200 OK):**
```json
{
  "dreamId": 1,
  "analysis": "A newly generated analysis for the dream...",
  "analyzedAt": 1748736900000,
  "analysisVersion": "v1",
  "analysisStatus": "COMPLETED"
}
```

**Process:**
1. Retrieves dream from database by ID
2. Bypasses any cached completed analysis
3. Sends dream content to the external Python analysis API
4. Replaces the previous successful result only when the new analysis succeeds

If reanalysis fails, the previous successful analysis result remains stored and the status is
updated to `FAILED`.

**Status Codes:**
- 200 OK - Reanalysis successful
- 404 Not Found - Dream with given ID does not exist
- 502 Bad Gateway - Analysis API error
- 500 Internal Server Error - Database error

---

## Data Models

### Dream Entry

**Fields:**
- `id` (int): Auto-generated primary key
- `title` (string): Dream title
- `content` (string): Full dream description
- `dreamDate` (string): Date in format `yyyy-MM-dd HH:mm:ss`
- `timestamp` (long): Unix milliseconds timestamp
- `symbolTags` (List<DreamSymbol>): Auto-extracted dream symbols
- `dreamType` (DreamType): Dream category
- `analyzed` (boolean): Compatibility flag derived from `analysisStatus == COMPLETED`
- `analysisResult` (string): Persisted full analysis text
- `analyzedAt` (long): Unix milliseconds timestamp for the last successful analysis
- `analysisVersion` (string): Analysis implementation/version identifier
- `analysisStatus` (AnalysisStatus): Current state (`PENDING`, `COMPLETED`, `FAILED`)

### DreamSymbol (Enum)

Supported symbols (auto-extracted from content):
- `FIRE` - Fire, flames, heat, sun
- `WATER` - Water, ocean, rain
- `CAT` - Cats, felines
- `EYE` - Eyes, sight, vision
- `DEATH` - Death, decay, endings
- `PORTAL` - Portals, doors, passages
- `SKY` - Sky, flying, birds
- `UNKNOWN` - No recognized symbols

### DreamType (Enum)

- `LUCID` - Lucid dreaming
- `NIGHTMARE` - Frightening dream
- `RECURRING` - Repeating dream
- `VISION` - Spiritual or prophetic dream
- `ORDINARY` - Regular dream
- `NONE` - Unclassified

## Running the API

### Prerequisites

- Java 17 or higher
- Gradle (or use `./gradlew`)
- SQLite JDBC driver (included in Gradle dependencies)
- GSON library (included in Gradle dependencies)

### Start the Server

```bash
./gradlew run
```

Server will start on `http://localhost:8080`

Log output:
```
INFO: DreamGrid REST API server started on http://localhost:8080
INFO: Press Ctrl+C to stop the server
```

### Stop the Server

Press `Ctrl+C` in the terminal.

## Example Usage

### Create a dream

```bash
curl -X POST http://localhost:8080/dreams \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Mountain Climb",
    "content": "I was climbing a tall mountain under a clear sky with fire on the horizon",
    "date": "2026-05-31",
    "type": "VISION"
  }'
```

### List all dreams

```bash
curl http://localhost:8080/dreams
```

### Get a specific dream

```bash
curl http://localhost:8080/dreams/1
```

### Analyze a dream (requires Python analysis service running on port 5005)

```bash
curl -X POST http://localhost:8080/dreams/1/analyze
```

Start the Python analysis service with:

```bash
python python/analysis_api.py
```

## Error Response Format

All error responses follow this format:

```json
{
  "statusCode": 400,
  "message": "Title cannot be empty"
}
```

## Database

- **Location:** `data/dreams.db`
- **Type:** SQLite
- **Auto-creation:** Database and tables created automatically on first run

### Database Schema

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

## Dependencies

```gradle
implementation 'org.xerial:sqlite-jdbc:3.50.2.0'
implementation 'com.google.code.gson:gson:2.10.1'
```
