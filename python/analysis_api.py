from flask import Flask, jsonify, request

import config
from analysis_service import DreamAnalysisService


def create_app(analysis_service: DreamAnalysisService = None) -> Flask:
    app = Flask(__name__)
    service = analysis_service or DreamAnalysisService()

    @app.get("/health")
    def health():
        return jsonify({"status": "ok", "modelVersion": config.MODEL_VERSION})

    @app.post("/analyze")
    def analyze():
        payload = request.get_json(silent=True)
        if not isinstance(payload, dict):
            return jsonify({"error": "Request body must be a JSON object"}), 400

        dream_text = payload.get("dream")
        if not isinstance(dream_text, str) or not dream_text.strip():
            return jsonify({"error": "Field 'dream' is required and must be a non-empty string"}), 400

        try:
            result = service.analyze(dream_text.strip())
            return jsonify(result.to_dict())
        except Exception:
            app.logger.exception("Dream analysis failed")
            return jsonify({"error": "Internal analysis failure"}), 500

    return app


if __name__ == "__main__":
    app = create_app()
    app.run(host=config.HOST, port=config.PORT)
