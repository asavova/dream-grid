from typing import Dict, List, Optional

import config
from models.analysis_result import AnalysisResult


class DreamAnalysisService:
    SYMBOL_KEYWORDS: Dict[str, List[str]] = {
        "FIRE": ["fire", "flame", "burning", "sun", "heat"],
        "WATER": ["water", "ocean", "rain", "river", "lake", "sea"],
        "CAT": ["cat", "kitten", "feline"],
        "EYE": ["eye", "eyes", "vision", "watching", "seeing"],
        "DEATH": ["death", "dead", "dying", "grave", "decay"],
        "PORTAL": ["portal", "door", "gate", "threshold", "passage"],
        "SKY": ["sky", "cloud", "flying", "flight", "bird"],
    }

    THEME_KEYWORDS: Dict[str, List[str]] = {
        "transition": ["portal", "door", "gate", "path", "journey", "threshold"],
        "fear": ["nightmare", "afraid", "fear", "chased", "falling", "dark"],
        "clarity": ["eye", "vision", "light", "clear", "seeing"],
        "change": ["fire", "death", "burning", "storm", "decay"],
        "freedom": ["sky", "flying", "bird", "open"],
        "emotion": ["water", "rain", "ocean", "river", "crying"],
    }

    def __init__(self, model_client=None, model_version: str = config.MODEL_VERSION):
        self.model_client = model_client or TransformersAnalysisModel()
        self.model_version = model_version

    def analyze(self, dream_text: str) -> AnalysisResult:
        summary = self.model_client.generate_summary(dream_text)
        detected_symbols = self.extract_symbols(dream_text)
        detected_themes = self.detect_themes(dream_text, detected_symbols)
        confidence_score = self.calculate_confidence_score(summary, detected_symbols)

        return AnalysisResult(
            summary=summary,
            detectedSymbols=detected_symbols,
            detectedThemes=detected_themes,
            confidenceScore=confidence_score,
            modelVersion=self.model_version,
        )

    def extract_symbols(self, dream_text: str) -> List[str]:
        normalized_text = dream_text.lower()
        symbols = [
            symbol
            for symbol, keywords in self.SYMBOL_KEYWORDS.items()
            if any(keyword in normalized_text for keyword in keywords)
        ]
        return symbols or ["UNKNOWN"]

    def detect_themes(self, dream_text: str, detected_symbols: List[str]) -> List[str]:
        normalized_text = dream_text.lower()
        themes = {
            theme
            for theme, keywords in self.THEME_KEYWORDS.items()
            if any(keyword in normalized_text for keyword in keywords)
        }

        if "UNKNOWN" not in detected_symbols and not themes:
            themes.add("symbolic-pattern")

        return sorted(themes)

    def calculate_confidence_score(self, summary: str, detected_symbols: List[str]) -> float:
        score = 0.45
        if summary and summary.strip():
            score += 0.3
        if "UNKNOWN" not in detected_symbols:
            score += min(len(detected_symbols) * 0.05, 0.2)
        return round(min(score, 0.95), 2)


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

    def generate_summary(self, dream_text: str) -> str:
        import torch

        prompt = f"Interpret this dream symbolically and personally:\n{dream_text}"
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
