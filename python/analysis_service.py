import config
from backends import create_backend
from models.analysis_result import AnalysisResult


class DreamAnalysisService:
    def __init__(self, backend=None, backend_name: str = None):
        self.backend = backend or create_backend(backend_name or config.ANALYSIS_BACKEND)

    @property
    def backend_name(self) -> str:
        return getattr(self.backend, "name", self.backend.__class__.__name__)

    @property
    def model_version(self) -> str:
        return getattr(self.backend, "model_version", config.MODEL_VERSION)

    def analyze(self, dream_text: str) -> AnalysisResult:
        return self.backend.analyze(dream_text)

    def answer_question(self, dream_text: str, analysis_result: str, question: str) -> str:
        return self.backend.answer_question(dream_text, analysis_result, question)
