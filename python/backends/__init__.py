from .rule_based_backend import RuleBasedAnalysisBackend
from .transformers_backend import TransformersAnalysisBackend


def create_backend(name: str = "rule-based"):
    backend_name = (name or "rule-based").strip().lower()

    if backend_name in {"rule-based", "rule_based", "heuristic"}:
        return RuleBasedAnalysisBackend()
    if backend_name in {"transformers", "huggingface"}:
        return TransformersAnalysisBackend()

    raise ValueError(f"Unsupported analysis backend: {name}")
