import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import config
from backends.rule_based_backend import RuleBasedAnalysisBackend
from rule_loader import RuleLoadError, load_classification_rules, load_content_safety_rules


class RuleLoaderTest(unittest.TestCase):
    def test_rule_files_load_successfully(self):
        interpretation = RuleBasedAnalysisBackend(model_version="test").rules
        safety = load_content_safety_rules(config.CONTENT_SAFETY_RULES_PATH)
        classification = load_classification_rules(config.CLASSIFICATION_RULES_PATH)

        self.assertGreater(len(interpretation["symbols"]), 0)
        self.assertGreater(len(safety["categories"]), 0)
        self.assertGreater(len(classification["classifications"]), 0)

    def test_malformed_rule_file_produces_clear_error(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "bad.json"
            path.write_text("{bad json", encoding="utf-8")

            with self.assertRaisesRegex(RuleLoadError, "malformed JSON"):
                load_content_safety_rules(path)

    def test_rule_based_analysis_detects_symbols_by_aliases(self):
        backend = RuleBasedAnalysisBackend(model_version="test")

        result = backend.analyze("Rain fell beside the ocean.")

        self.assertIn("water", result.detectedSymbols)

    def test_rule_based_analysis_returns_deterministic_themes(self):
        backend = RuleBasedAnalysisBackend(model_version="test")

        first = backend.analyze("I opened a portal under the sky.")
        second = backend.analyze("I opened a portal under the sky.")

        self.assertEqual(first.detectedThemes, second.detectedThemes)
        self.assertIn("transition", first.detectedThemes)

    def test_default_summary_is_used_when_no_symbol_matches(self):
        backend = RuleBasedAnalysisBackend(model_version="test")

        result = backend.analyze("A quiet ordinary moment.")

        self.assertEqual(backend.rules["defaultSummary"], result.summary)

    def test_content_safety_blocks_configured_unsafe_content(self):
        rules = load_content_safety_rules(config.CONTENT_SAFETY_RULES_PATH)
        keywords = [
            keyword
            for category in rules["categories"]
            for keyword in category["keywords"]
        ]

        self.assertIn("build a bomb", keywords)

    def test_safe_dream_content_is_not_blocked_by_rules(self):
        rules = load_content_safety_rules(config.CONTENT_SAFETY_RULES_PATH)
        safe_text = "I walked through a quiet forest."

        blocked = any(
            keyword in safe_text
            for category in rules["categories"]
            for keyword in category["keywords"]
        )

        self.assertFalse(blocked)

    def test_classification_rules_detect_lucid_nightmare_and_neutral(self):
        rules = load_classification_rules(config.CLASSIFICATION_RULES_PATH)
        by_type = {rule["type"]: rule for rule in rules["classifications"]}

        self.assertIn("realized i was dreaming", by_type["LUCID"]["keywords"])
        self.assertIn("panic", by_type["NIGHTMARE"]["keywords"])
        self.assertEqual([], by_type["NEUTRAL"]["keywords"])


if __name__ == "__main__":
    unittest.main()
