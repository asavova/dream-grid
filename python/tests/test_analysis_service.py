import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from analysis_service import DreamAnalysisService
from backends.rule_based_backend import RuleBasedAnalysisBackend


class DreamAnalysisServiceTest(unittest.TestCase):
    def test_rule_based_backend_returns_deterministic_structured_output(self):
        service = DreamAnalysisService(
            backend=RuleBasedAnalysisBackend(model_version="test-rule-backend")
        )

        first = service.analyze("I walked through a portal under a bright sky.")
        second = service.analyze("I walked through a portal under a bright sky.")

        self.assertEqual(first, second)
        self.assertIn("sky", first.detectedSymbols)
        self.assertIn("door", first.detectedSymbols)
        self.assertIn("transition", first.detectedThemes)
        self.assertEqual("test-rule-backend", first.modelVersion)
        self.assertGreater(first.confidenceScore, 0.0)

    def test_default_backend_does_not_import_transformers(self):
        sys.modules.pop("torch", None)
        sys.modules.pop("transformers", None)

        service = DreamAnalysisService(backend_name="rule-based")
        result = service.analyze("A fire burned beside the road.")

        self.assertEqual("rule-based", service.backend_name)
        self.assertIn("fire", result.detectedSymbols)
        self.assertNotIn("torch", sys.modules)
        self.assertNotIn("transformers", sys.modules)

    def test_ask_returns_deterministic_answer_from_selected_backend(self):
        service = DreamAnalysisService(
            backend=RuleBasedAnalysisBackend(model_version="test-rule-backend")
        )

        answer = service.answer_question(
            "I walked through a portal.",
            '{"summary":"A transition dream."}',
            "What symbols are present?",
        )

        self.assertEqual("The strongest detected symbols are: door.", answer)


if __name__ == "__main__":
    unittest.main()
