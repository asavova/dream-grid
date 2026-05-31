import re
from typing import List

import config
from models.analysis_result import AnalysisResult


class RuleBasedAnalysisBackend:
    name = "rule-based"

    SYMBOL_TERMS = {
        "fire": ["fire", "flame", "burn", "smoke"],
        "water": ["water", "ocean", "sea", "river", "rain"],
        "sky": ["sky", "cloud", "stars", "moon", "sun"],
        "door": ["door", "gate", "portal", "entrance"],
        "road": ["road", "path", "bridge", "street"],
        "house": ["house", "home", "room", "building"],
        "forest": ["forest", "tree", "woods"],
        "shadow": ["shadow", "dark", "night"],
    }

    THEME_TERMS = {
        "change": ["fire", "storm", "move", "moving", "transform"],
        "transition": ["door", "gate", "portal", "road", "path", "bridge"],
        "uncertainty": ["lost", "dark", "fog", "maze", "unknown"],
        "freedom": ["fly", "flying", "sky", "open", "wide"],
        "pressure": ["chase", "running", "late", "trapped", "falling"],
        "reflection": ["mirror", "water", "home", "childhood"],
    }

    def __init__(self, model_version: str = None):
        self.model_version = model_version or config.MODEL_VERSION

    def analyze(self, dream_text: str) -> AnalysisResult:
        text = self._normalize(dream_text)
        symbols = self._match_terms(text, self.SYMBOL_TERMS)
        themes = self._match_terms(text, self.THEME_TERMS)

        if not symbols:
            symbols = ["unknown"]
        if not themes:
            themes = ["reflection"]

        return AnalysisResult(
            summary=self._build_summary(symbols, themes),
            detectedSymbols=symbols,
            detectedThemes=themes,
            confidenceScore=self._confidence(symbols, themes),
            modelVersion=self.model_version,
        )

    def answer_question(self, dream_text: str, analysis_result: str, question: str) -> str:
        text = self._normalize(dream_text + " " + analysis_result)
        question_text = self._normalize(question)

        symbols = self._match_terms(text, self.SYMBOL_TERMS)
        themes = self._match_terms(text, self.THEME_TERMS)

        if "symbol" in question_text and symbols:
            return "The strongest detected symbols are: " + ", ".join(symbols) + "."
        if "theme" in question_text and themes:
            return "The main detected themes are: " + ", ".join(themes) + "."
        if symbols or themes:
            details = symbols or themes
            return "Based on the stored analysis, the dream points to " + ", ".join(details) + "."

        return "The stored analysis does not contain enough detail to answer that confidently."

    def _match_terms(self, text: str, vocabulary: dict) -> List[str]:
        matches = []
        for label, terms in vocabulary.items():
            if any(self._contains_term(text, term) for term in terms):
                matches.append(label)
        return matches

    def _contains_term(self, text: str, term: str) -> bool:
        return re.search(rf"\b{re.escape(term)}\b", text) is not None

    def _normalize(self, value: str) -> str:
        return re.sub(r"\s+", " ", (value or "").lower()).strip()

    def _build_summary(self, symbols: List[str], themes: List[str]) -> str:
        symbol_text = ", ".join(symbols)
        theme_text = ", ".join(themes)
        return f"The dream contains symbolic material around {symbol_text}, with themes of {theme_text}."

    def _confidence(self, symbols: List[str], themes: List[str]) -> float:
        score = 0.45
        if symbols and symbols != ["unknown"]:
            score += 0.2
        if themes:
            score += 0.15
        if len(symbols) + len(themes) >= 4:
            score += 0.1
        return round(min(score, 0.85), 2)
