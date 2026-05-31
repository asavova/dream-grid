import unittest
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from analysis_service import DreamAnalysisService


class StaticModel:
    def generate_text(self, prompt: str) -> str:
        if "Question:" in prompt:
            return "The portal points to a transition."
        return """
{
  "summary": "A concise symbolic interpretation.",
  "detectedSymbols": ["portal", "sky"],
  "detectedThemes": ["transition", "freedom"],
  "confidenceScore": 0.82
}
"""


class DreamAnalysisServiceTest(unittest.TestCase):
    def test_analyze_returns_structured_result(self):
        service = DreamAnalysisService(model_client=StaticModel(), model_version="test-version")

        result = service.analyze("I walked through a portal under a bright sky.")

        self.assertEqual("A concise symbolic interpretation.", result.summary)
        self.assertEqual(["portal", "sky"], result.detectedSymbols)
        self.assertIn("transition", result.detectedThemes)
        self.assertIn("freedom", result.detectedThemes)
        self.assertEqual("test-version", result.modelVersion)
        self.assertEqual(0.82, result.confidenceScore)

    def test_answer_question_uses_model_context(self):
        service = DreamAnalysisService(model_client=StaticModel())

        answer = service.answer_question(
            "I walked through a portal.",
            '{"summary":"A transition dream."}',
            "What does the portal mean?",
        )

        self.assertEqual("The portal points to a transition.", answer)


if __name__ == "__main__":
    unittest.main()
