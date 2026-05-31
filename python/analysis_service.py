import json
import re
from typing import List, Optional

import config
from models.analysis_result import AnalysisResult


class DreamAnalysisService:
    def __init__(self, model_client=None, model_version: str = config.MODEL_VERSION):
        self.model_client = model_client or TransformersAnalysisModel()
        self.model_version = model_version

    def analyze(self, dream_text: str) -> AnalysisResult:
        prompt = self.build_analysis_prompt(dream_text)
        model_output = self.model_client.generate_text(prompt)
        payload = self.parse_json_object(model_output)

        return AnalysisResult(
            summary=self.normalize_text(payload.get("summary")),
            detectedSymbols=self.normalize_list(payload.get("detectedSymbols")),
            detectedThemes=self.normalize_list(payload.get("detectedThemes")),
            confidenceScore=self.normalize_confidence(payload.get("confidenceScore"), payload),
            modelVersion=self.model_version,
        )

    def answer_question(self, dream_text: str, analysis_result: str, question: str) -> str:
        prompt = self.build_question_prompt(dream_text, analysis_result, question)
        answer = self.model_client.generate_text(prompt)
        return self.normalize_answer(answer)

    def build_analysis_prompt(self, dream_text: str) -> str:
        return f"""
Analyze the dream below and return only valid JSON. Do not include markdown.

Required JSON shape:
{{
  "summary": "short interpretation in plain language",
  "detectedSymbols": ["symbol mentioned or strongly implied by the dream"],
  "detectedThemes": ["theme inferred from the dream"],
  "confidenceScore": 0.0
}}

Rules:
- Symbols must come from the dream text, not from a fixed taxonomy.
- Themes may be inferred, but keep them concise.
- confidenceScore is your confidence that the interpretation is grounded in the dream text.
- Use a number between 0 and 1 for confidenceScore.
- If the dream is unclear, return a lower confidenceScore.

Dream:
\"\"\"{dream_text}\"\"\"
""".strip()

    def build_question_prompt(self, dream_text: str, analysis_result: str, question: str) -> str:
        return f"""
Answer the user's question using only the dream and stored analysis below.
If the answer is uncertain, say what is uncertain. Keep the answer concise.

Dream:
\"\"\"{dream_text}\"\"\"

Stored analysis:
\"\"\"{analysis_result}\"\"\"

Question:
\"\"\"{question}\"\"\"
""".strip()

    def parse_json_object(self, model_output: str) -> dict:
        try:
            return json.loads(model_output)
        except json.JSONDecodeError:
            match = re.search(r"\{.*\}", model_output, flags=re.DOTALL)
            if match:
                return json.loads(match.group(0))
            raise ValueError("Analysis model did not return a JSON object")

    def normalize_text(self, value) -> str:
        if isinstance(value, str) and value.strip():
            return value.strip()
        return "The model did not provide a usable summary."

    def normalize_list(self, value) -> List[str]:
        if not isinstance(value, list):
            return []

        normalized = []
        for item in value:
            if isinstance(item, str) and item.strip():
                normalized.append(item.strip())
        return normalized

    def normalize_confidence(self, value, payload: dict) -> float:
        if isinstance(value, (int, float)):
            return round(max(0.0, min(float(value), 1.0)), 2)

        score = 0.35
        if isinstance(payload.get("summary"), str) and payload.get("summary").strip():
            score += 0.25
        if self.normalize_list(payload.get("detectedSymbols")):
            score += 0.2
        if self.normalize_list(payload.get("detectedThemes")):
            score += 0.1
        return round(min(score, 0.75), 2)

    def normalize_answer(self, value: str) -> str:
        if value and value.strip():
            return value.strip()
        return "The model did not provide an answer."


class TransformersAnalysisModel:
    def __init__(
        self,
        model_name: str = config.MODEL_NAME,
        token: Optional[str] = config.HUGGINGFACE_TOKEN,
    ):
        from transformers import AutoModelForSeq2SeqLM, AutoTokenizer

        self.model_name = model_name
        self.tokenizer = AutoTokenizer.from_pretrained(model_name, token=token)
        self.model = AutoModelForSeq2SeqLM.from_pretrained(model_name, token=token)

    def generate_text(self, prompt: str) -> str:
        import torch

        inputs = self.tokenizer(
            prompt,
            add_special_tokens=False,
            return_tensors="pt",
            truncation=True,
            max_length=config.MAX_INPUT_LENGTH,
        )

        with torch.no_grad():
            outputs = self.model.generate(**inputs, max_length=config.MAX_OUTPUT_LENGTH)

        return self.tokenizer.decode(outputs[0], skip_special_tokens=True)
