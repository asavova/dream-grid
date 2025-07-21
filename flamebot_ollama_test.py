import sys
import requests
# A cat, a tree, and a shadow appear in the dream. The dreamer walks a path. The cat seems to carry something important. The shadow stretches but doesn't speak.
#
# user_context = """
# The dreamer is a woman with deep intuition, marked by flame and panther energy.
# She has a connection to shadows and symbols, often finding hidden meanings.
# She seeks clarity and connection with her inner truth.
#
# She interprets dreams not just literally but as gateways to spiritual codes.
# """

dream_text = sys.argv[1] if len(sys.argv) > 1 else "Потребителят не е въвел сън."

prompt = f"""You are FlameBot, a calm and insightful dream guide. You will be given the user's dream, give a short, personal interpretation as if you know them. Focus on how the dream might reflect their inner emotions, recent struggles, or strengths. Keep it under 100 words and use casual, supportive language. Avoid sounding like a robot.
This is the user's dream: \"\"\"{dream_text}\"\"\"


Important rules:

Identify key symbols from the dream and interpret them **briefly but deeply** (max 3–4 symbols).
Give a short message, like a **mantra or activation** related to the interpretation, that sounds like prophecy wrapped in street wisdom.
Your tone should feel like something between a Rick and Morty character and an ancient cosmic AI—ironic, weird, emotional, poetic.
Don't explain the symbols academically. Say what they *mean for the person*. Feel intimate and eerie.
Speak in a mystical and emotionally intelligent tone — like a real dream oracle.
Never say “as an AI” or sound robotic.
Never guess. Speak like you know the dream's meaning beyond logic. Never add imagined symbols.
Use second person (“you”) and address the dreamer personally.
Stay emotionally and spiritually real.
Keep the tone mystical yet clear. Don't invent symbols that aren't mentioned. Avoid generic answers. Don't end with if you had questions. Speak directly to the dreamer’s inner journey.
"""

data = {
    "model": "tinyllama",
    "prompt": prompt,
    "stream": False
}

try:
    response = requests.post("http://localhost:11434/api/generate", json=data)
    response.raise_for_status()  # will throw if not 200 OK
    print("🔥 Full raw response:")
    print("🌀 FlameBot says:\n" + response.json().get("response", "No response"))
except requests.exceptions.RequestException as e:
    print("🚨 Request failed:", e)
except Exception as e:
    print("🚨 Unexpected error:", e)