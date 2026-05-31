import unittest
import importlib.util
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from models.analysis_result import AnalysisResult

if importlib.util.find_spec("flask") is not None:
    from analysis_api import create_app
else:
    create_app = None


class StaticService:
    def analyze(self, dream_text: str) -> AnalysisResult:
        return AnalysisResult(
            summary="Structured interpretation.",
            detectedSymbols=["FIRE"],
            detectedThemes=["change"],
            confidenceScore=0.8,
            modelVersion="test-version",
        )


class FailingService:
    def analyze(self, dream_text: str) -> AnalysisResult:
        raise RuntimeError("model unavailable")


@unittest.skipIf(create_app is None, "Flask is not installed")
class AnalysisApiTest(unittest.TestCase):
    def test_health(self):
        client = create_app(StaticService()).test_client()

        response = client.get("/health")

        self.assertEqual(200, response.status_code)
        self.assertEqual("ok", response.get_json()["status"])

    def test_analyze_valid_request(self):
        client = create_app(StaticService()).test_client()

        response = client.post("/analyze", json={"dream": "A dream about fire."})

        self.assertEqual(200, response.status_code)
        self.assertEqual("Structured interpretation.", response.get_json()["summary"])

    def test_analyze_invalid_request(self):
        client = create_app(StaticService()).test_client()

        response = client.post("/analyze", json={})

        self.assertEqual(400, response.status_code)

    def test_analyze_internal_failure(self):
        client = create_app(FailingService()).test_client()

        response = client.post("/analyze", json={"dream": "A dream about fire."})

        self.assertEqual(500, response.status_code)


if __name__ == "__main__":
    unittest.main()
