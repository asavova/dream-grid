import re
from typing import Dict, List

import config
from models.analysis_result import AnalysisResult
from rule_loader import load_interpretation_rules


class RuleBasedAnalysisBackend:
    name = "rule-based"

    def __init__(self, model_version: str = None, rules: Dict = None):
        self.model_version = model_version or config.MODEL_VERSION
        self.rules = rules or load_interpretation_rules(config.DREAM_INTERPRETATION_RULES_PATH)

    def analyze(self, dream_text: str) -> AnalysisResult:
        text = self._normalize(dream_text)
        symbols = self._detect_symbols(text)
        themes = self._detect_themes(text, symbols)

        if not themes:
            themes = list(self.rules.get("defaultThemes", ["reflection"]))

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
        symbols = self._detect_symbols(text)
        themes = self._detect_themes(text, symbols)

        if "symbol" in question_text and symbols:
            return "The strongest detected symbols are: " + ", ".join(symbols) + "."
        if "theme" in question_text and themes:
            return "The main detected themes are: " + ", ".join(themes) + "."
        if symbols or themes:
            details = symbols or themes
            return "Based on the stored analysis, the dream points to " + ", ".join(details) + "."

        return "The stored analysis does not contain enough detail to answer that confidently."

    def _detect_symbols(self, text: str) -> List[str]:
        matches = []
        for rule in self.rules.get("symbols", []):
            aliases = rule.get("aliases", [])
            if any(self._contains_term(text, alias) for alias in aliases):
                matches.append(rule["tag"])
        return matches

    def _detect_themes(self, text: str, symbols: List[str]) -> List[str]:
        themes = []
        symbol_rules = {
            rule["tag"]: rule for rule in self.rules.get("symbols", []) if "tag" in rule
        }
        for symbol in symbols:
            for theme in symbol_rules.get(symbol, {}).get("themes", []):
                if theme not in themes:
                    themes.append(theme)

        for mapping in self.rules.get("themeMappings", []):
            aliases = mapping.get("aliases", [])
            theme = mapping.get("theme")
            if theme and theme not in themes and any(self._contains_term(text, alias) for alias in aliases):
                themes.append(theme)
        return themes

    def _contains_term(self, text: str, term: str) -> bool:
        return re.search(rf"\b{re.escape(term.lower())}\b", text) is not None

    def _normalize(self, value: str) -> str:
        return re.sub(r"\s+", " ", (value or "").lower()).strip()

    def _build_summary(self, symbols: List[str], themes: List[str]) -> str:
        if not symbols:
            return self.rules["defaultSummary"]
        template = self.rules.get(
            "summaryTemplate",
            "The dream contains symbolic material around {symbols}, with themes of {themes}.",
        )
        return template.format(symbols=", ".join(symbols), themes=", ".join(themes))

    def _confidence(self, symbols: List[str], themes: List[str]) -> float:
        score = 0.45
        if symbols:
            score += 0.2
        if themes:
            score += 0.15
        if len(symbols) + len(themes) >= 4:
            score += 0.1
        return round(min(score, 0.85), 2)
