from dotenv import load_dotenv
from flask import Flask, request, jsonify

from flamebot import FlameBot

load_dotenv()

app = Flask(__name__)
bot = FlameBot()
print("✅ FlameBot model loaded")

@app.route("/")
def index():
    return "🔥 FlameBot Dream Analyzer API is running."

@app.route("/analyze", methods=["POST"])
def analyze():
    data = request.json
    dream_text = data.get('dream', '')
    if not dream_text:
        return jsonify({'error': 'No dream provided'}), 400

    interpretation = bot.analyze(dream_text)
    return jsonify({'interpretation': interpretation})

if __name__ == "__main__":
    print("🚀 Starting Flask server on http://0.0.0.0:5005")
    app.run(host="0.0.0.0", port=5005)
