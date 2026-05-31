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
  "classification": "NEUTRAL",
  "tags": ["mountain", "clear sky"]
}
```

New dreams are stored with `analysisStatus` set to `PENDING`.

Validation rules:

- `title` must not be blank and must be at most 160 characters.
- `content` must not be blank and must be at most 12,000 characters.
- `date`, when provided, must use `yyyy-MM-dd` or `yyyy-MM-dd HH:mm:ss`.
- `classification`, when provided, must be one of `LUCID`, `NIGHTMARE`, `RECURRING`, `NEUTRAL`, or `UNKNOWN`. The older `type` field is still accepted as a request alias for compatibility.
- `tags`, when provided, are trimmed, lowercased, deduplicated, and stored as manual tags.

## Read Dreams

```http
GET /dreams
GET /dreams/{id}
```

`GET /dreams/{id}` returns `404` when the dream does not exist.

`GET /dreams` also supports filters:

```http
GET /dreams?classification=NEUTRAL&status=COMPLETED&tag=sky
```

Filters can be used individually or together. The older `type` query parameter is still accepted as a classification alias.

## Search Dreams

```http
GET /dreams/search?query=ocean
```

Search checks both `title` and `content`.

## Dreams by Tag

```http
GET /dreams/tags/sky
```

Tags are normalized before querying. A tag with no linked dreams returns an empty list.

## Tag Counts

```http
GET /tags
```

Example response:

```json
[
  {
    "tag": "fire",
    "normalizedName": "fire",
    "count": 2
  },
  {
    "tag": "clear sky",
    "normalizedName": "clear sky",
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

## Dream Classification

```http
GET /dreams/{id}/classification
PUT /dreams/{id}/classification
DELETE /dreams/{id}/classification/override
```

`GET /dreams/{id}/classification` returns the stored classification state:

```json
{
  "userClassification": null,
  "inferredClassification": "NIGHTMARE",
  "effectiveClassification": "NIGHTMARE",
  "classificationSource": "ANALYSIS",
  "classificationReason": "Analysis or dream content contains nightmare-related signals.",
  "classificationUpdatedAt": 1780250100000
}
```

`PUT /dreams/{id}/classification` sets a user override:

```json
{
  "classification": "LUCID"
}
```

User overrides become the effective classification and are preserved during reanalysis. `DELETE /dreams/{id}/classification/override` removes the user override and restores the inferred classification when one exists.

After successful analysis, the backend may infer `LUCID`, `NIGHTMARE`, or `NEUTRAL` from the analysis response and dream content. It may infer `RECURRING` only from historical tag overlap with other saved dreams. This workflow is deterministic backend logic and does not call the Python service beyond the normal analysis request.

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

Symbols and themes from the model response are normalized into dynamic analysis-generated tags. Reanalysis replaces analysis-generated tags for that dream and preserves manual tags.

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
