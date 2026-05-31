# API

The Java backend runs on `http://localhost:8080`. Analysis endpoints require the Python service on `http://127.0.0.1:5005`.

## Health

```http
GET /health
```

```json
{
  "status": "ok"
}
```

## Create Dream

```http
POST /dreams
Content-Type: application/json
```

```json
{
  "title": "Mountain Climb",
  "content": "I was climbing a mountain under a clear sky",
  "date": "2026-05-31",
  "type": "VISION"
}
```

New dreams are stored with `analysisStatus` set to `PENDING`.

Validation rules:

- `title` must not be blank and must be at most 160 characters.
- `content` must not be blank and must be at most 12,000 characters.
- `date`, when provided, must use `yyyy-MM-dd` or `yyyy-MM-dd HH:mm:ss`.
- `type`, when provided, must match a known dream type.

## Read Dreams

```http
GET /dreams
GET /dreams/{id}
```

`GET /dreams/{id}` returns `404` when the dream does not exist.

`GET /dreams` also supports filters:

```http
GET /dreams?type=VISION&status=COMPLETED&tag=sky
```

Filters can be used individually or together.

## Search Dreams

```http
GET /dreams/search?query=ocean
```

Search checks both `title` and `content`.

## Dreams by Tag

```http
GET /dreams/tags/sky
```

Tags are normalized before querying. Unknown tags return an empty list.

## Tag Counts

```http
GET /tags
```

Example response:

```json
[
  {
    "tag": "fire",
    "count": 2
  },
  {
    "tag": "sky",
    "count": 1
  }
]
```

## Analyze Dream

```http
POST /dreams/{id}/analyze
```

If a completed analysis exists and no expected analysis version is configured, the stored result is returned. If `DREAMGRID_ANALYSIS_VERSION` is configured, cached analysis is reused only when the stored version matches that value. New analysis stores the `modelVersion` returned by the Python service.

Example response:

```json
{
  "dreamId": 1,
  "analysis": "{\"summary\":\"A concise interpretation.\",\"detectedSymbols\":[\"SKY\"],\"detectedThemes\":[\"freedom\"],\"confidenceScore\":0.82,\"modelVersion\":\"rule-based\"}",
  "analyzedAt": 1780250100000,
  "analysisVersion": "rule-based",
  "analysisStatus": "COMPLETED"
}
```

If analysis fails, the status is set to `FAILED`. A previous successful analysis is not deleted.

## Force Reanalysis

```http
POST /dreams/{id}/reanalyze
```

This bypasses the cache and calls the Python service again.

## Ask a Question

```http
POST /dreams/{id}/questions
Content-Type: application/json
```

```json
{
  "question": "What does the sky represent?"
}
```

Example response:

```json
{
  "dreamId": 1,
  "question": "What does the sky represent?",
  "answer": "The sky points to openness or a need for perspective in the dream."
}
```

This endpoint requires a completed analysis. If the dream is not analyzed yet, the API returns `400`.

Questions must not be blank and must be at most 1,000 characters.

## Python Analysis Service

The Python service exposes:

```http
GET /health
POST /analyze
POST /ask
```

`GET /health` returns:

```json
{
  "status": "ok",
  "backend": "rule-based",
  "modelVersion": "rule-based"
}
```

`POST /analyze` returns:

```json
{
  "summary": "A concise interpretation of the dream.",
  "detectedSymbols": ["SKY"],
  "detectedThemes": ["freedom"],
  "confidenceScore": 0.82,
  "modelVersion": "rule-based"
}
```

`POST /analyze` and `POST /ask` accept the existing `dream` field. `content` is also accepted as an alias for the dream text.

`confidenceScore` is the model's confidence that the analysis is grounded in the dream text. It is a useful signal, not a guarantee.

Known symbols from the model response are normalized into the Java `DreamSymbol` enum before being persisted as `symbolTags`.

## Error Format

```json
{
  "error": "VALIDATION_ERROR",
  "message": "Validation message"
}
```

Current error codes:

- `VALIDATION_ERROR`
- `NOT_FOUND`
- `ANALYSIS_SERVICE_ERROR`
- `CONTENT_REJECTED`
- `INTERNAL_ERROR`

DreamGrid applies a deterministic application-level safety policy before calling the Python analysis service. The policy currently rejects supported categories such as self-harm instructions, illegal instructions, explicit sexual content, graphic violence, and hate or harassment. Rejected content returns `CONTENT_REJECTED` and is not sent to the analysis service.
