import unittest
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from analysis_service import DreamAnalysisService


class StaticModel:
    def generate_summary(self, dream_text: str) -> str:
        return "A concise symbolic interpretation."


class DreamAnalysisServiceTest(unittest.TestCase):
    def test_analyze_returns_structured_result(self):
        service = DreamAnalysisService(model_client=StaticModel(), model_version="test-version")

        result = service.analyze("I walked through a portal under a bright sky.")

        self.assertEqual("A concise symbolic interpretation.", result.summary)
        self.assertEqual(["PORTAL", "SKY"], result.detectedSymbols)
        self.assertIn("transition", result.detectedThemes)
        self.assertIn("freedom", result.detectedThemes)
        self.assertEqual("test-version", result.modelVersion)
        self.assertGreater(result.confidenceScore, 0)

    def test_unknown_symbol_when_no_keywords_match(self):
        service = DreamAnalysisService(model_client=StaticModel())

        result = service.analyze("I sat in a quiet room.")

        self.assertEqual(["UNKNOWN"], result.detectedSymbols)


if __name__ == "__main__":
    unittest.main()
