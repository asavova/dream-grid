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
  "type": "NEUTRAL",
  "tags": ["mountain", "clear sky"]
}
```

New dreams are stored with `analysisStatus` set to `PENDING`.

Validation rules:

- `title` must not be blank and must be at most 160 characters.
- `content` must not be blank and must be at most 12,000 characters.
- `date`, when provided, must use `yyyy-MM-dd` or `yyyy-MM-dd HH:mm:ss`.
- `type`, when provided, must be one of `LUCID`, `NIGHTMARE`, `RECURRING`, `NEUTRAL`, or `UNKNOWN`.
- `tags`, when provided, are trimmed, lowercased, deduplicated, and stored as manual tags.

Type behavior:

- If `type` is provided, it is stored as `userType` and becomes `effectiveType`.
- If `type` is omitted, the dream starts with `effectiveType` set to `UNKNOWN`.
- `typeSource` is `USER`, `ANALYSIS`, `PATTERN_ENGINE`, or `UNKNOWN`.
- `inferredType` is populated during analysis/reanalysis when deterministic rules match.

## Read Dreams

```http
GET /dreams
GET /dreams/{id}
```

`GET /dreams/{id}` returns `404` when the dream does not exist.

## Update Dream

```http
PUT /dreams/{id}
Content-Type: application/json
```

```json
{
  "title": "Updated title",
  "content": "Updated content",
  "date": "2026-06-01",
  "type": "NEUTRAL"
}
```

All fields are optional in the request. Missing fields keep their current values. The final merged payload is validated with the same rules as dream creation.

Lifecycle behavior:

- If only `title`, `date`, or `type` changes, the latest analysis snapshot is kept.
- If `content` changes, `analysisStatus` becomes `STALE`.
- Stale dreams do not reuse cached analysis.
- Existing analysis-generated tags are removed.
- Manual tags are preserved.
- Analysis run history is unchanged.

## Delete Dream

```http
DELETE /dreams/{id}
```

Removes the dream and cascades deletion to related `analysis_runs` and `dream_tag_links`. Orphaned tag rows are cleaned up from `dream_tags`. Missing dreams return `404`.

`GET /dreams` also supports filters:

```http
GET /dreams?query=ocean&type=NEUTRAL&status=COMPLETED&tag=sky
```

Filters can be used individually or together. `query` searches `title` and `content`; `q` is also accepted as a query alias.

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

## Tag Insights

```http
GET /insights/tags
GET /insights/recurring
GET /insights/co-occurrences
GET /insights/tags/{tag}
```

`GET /insights/tags` returns frequent normalized tags using unique dream presence per tag.

`GET /insights/recurring` returns only tags that appear in more than one dream.

`GET /insights/co-occurrences` returns stable tag pairs independent of input order. Pairs are sorted by `count` descending, then tag names ascending.

`GET /insights/tags/{tag}` returns:

- normalized tag name
- usage count
- related tags with co-occurrence counts
- recent dream IDs for that tag

Insight rules:

- tag matching is case-insensitive through normalization
- duplicate links for the same tag in one dream do not inflate counts
- empty databases return empty lists, not errors

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
Each real analysis attempt is also stored in `analysis_runs`.

## Force Reanalysis

```http
POST /dreams/{id}/reanalyze
```

This bypasses the cache and calls the Python service again.

## Analysis History

```http
GET /dreams/{id}/analyses
GET /dreams/{id}/analyses/latest
```

`GET /dreams/{id}/analyses` returns all stored analysis attempts for the dream, newest first:

```json
[
  {
    "id": 2,
    "dreamId": 1,
    "requestedAt": 1780250200000,
    "completedAt": 1780250201200,
    "status": "COMPLETED",
    "analysisVersion": "rule-based",
    "analysisResult": "{\"summary\":\"A concise interpretation.\"}",
    "failureReason": null
  }
]
```

`GET /dreams/{id}/analyses/latest` returns the most recent analysis run. Failed runs are included in history with `status` set to `FAILED` and `failureReason` populated. The latest successful analysis snapshot remains on the dream row for backward compatibility.

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
  "id": 10,
  "dreamId": 1,
  "analysisRunId": 7,
  "question": "What does the sky represent?",
  "answer": "The sky points to openness or a need for perspective in the dream.",
  "createdAt": 1780250300000
}
```

This endpoint requires a completed analysis. If the dream is not analyzed yet, the API returns `400`.
A question is persisted only after the answer is successfully generated.
Each stored question is linked to the completed `analysis_runs` row used to answer.

Questions must not be blank and must be at most 1,000 characters.

Question history endpoints:

```http
GET /dreams/{id}/questions
GET /dreams/{id}/questions/{questionId}
```

## Dream Classification

```http
GET /dreams/{id}/classification
PUT /dreams/{id}/classification
DELETE /dreams/{id}/classification/override
```

`GET /dreams/{id}/classification` returns the stored classification state:

```json
{
  "userType": null,
  "inferredType": "NIGHTMARE",
  "effectiveType": "NIGHTMARE",
  "typeSource": "ANALYSIS",
  "classificationReason": "Content indicates fear, threat, or panic signals.",
  "typeConfidence": 0.9,
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

Type inference is deterministic backend logic, not AI moderation. It uses explicit phrase and signal matching from dream text plus recurring tag overlap for recurring-type promotion during analysis workflows.
Text classification rules are loaded from `python/rules/classification_rules.json`. `RECURRING` is not inferred from text-only keywords; recurring promotion depends on historical tag overlap.

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

The default `rule-based` Python backend loads deterministic symbol, alias, theme, interpretation, and summary rules from `python/rules/dream_interpretation_rules.json`. Adding a new symbol means adding a JSON object with `tag`, `aliases`, `themes`, and `interpretation`.

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

DreamGrid applies a deterministic application-level safety policy before calling the Python analysis service. Safety categories, keyword lists, and rejection messages are loaded from `python/rules/content_safety_rules.json`. Rejected content returns `CONTENT_REJECTED` and is not sent to the analysis service.
