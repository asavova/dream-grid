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

## Read Dreams

```http
GET /dreams
GET /dreams/{id}
```

`GET /dreams/{id}` returns `404` when the dream does not exist.

## Analyze Dream

```http
POST /dreams/{id}/analyze
```

If a completed analysis exists for the current analysis version, the stored result is returned. Otherwise the backend calls the Python service and stores the response.

Example response:

```json
{
  "dreamId": 1,
  "analysis": "{\"summary\":\"A concise interpretation.\",\"detectedSymbols\":[\"SKY\"],\"detectedThemes\":[\"freedom\"],\"confidenceScore\":0.82,\"modelVersion\":\"v1\"}",
  "analyzedAt": 1780250100000,
  "analysisVersion": "v1",
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

This endpoint requires a completed analysis. If the dream is not analyzed yet, the API returns `409`.

## Python Analysis Service

The Python service exposes:

```http
GET /health
POST /analyze
POST /ask
```

`POST /analyze` returns:

```json
{
  "summary": "A concise interpretation of the dream.",
  "detectedSymbols": ["SKY"],
  "detectedThemes": ["freedom"],
  "confidenceScore": 0.82,
  "modelVersion": "v1"
}
```

`confidenceScore` is the model's confidence that the analysis is grounded in the dream text. It is a useful signal, not a guarantee.

## Error Format

```json
{
  "statusCode": 400,
  "message": "Validation message"
}
```
