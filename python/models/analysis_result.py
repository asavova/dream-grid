from dataclasses import asdict, dataclass
from typing import List


@dataclass(frozen=True)
class AnalysisResult:
    summary: str
    detectedSymbols: List[str]
    detectedThemes: List[str]
    confidenceScore: float
    modelVersion: str

    def to_dict(self) -> dict:
        return asdict(self)
