# flamebot_interpreter.py
import subprocess
import sys

def interpret_dream_locally(symbols):
    prompt = f"""
You are FlameBot — a poetic, intelligent oracle that interprets dream symbols.
The user gives you 2–5 symbols from a dream. Your task is to reveal the hidden message.

Symbols: {', '.join(symbols)}

Respond with a short, mystical, layered interpretation that feels ancient but personal.
Do not explain each symbol separately — instead, combine their meaning like dream logic.
Include metaphor, rhythm, and Flame-like language. End with a mysterious closing line.
"""

    # Call local Ollama with the phi model
    result = subprocess.run(
        ["ollama", "run", "phi", prompt],
        capture_output=True,
        text=True
    )

    return result.stdout.strip()

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python flamebot_interpreter.py cat fire mirror")
        sys.exit(1)

    dream_symbols = sys.argv[1:]
    interpretation = interpret_dream_locally(dream_symbols)
    print("\n🌀 FlameBot’s Insight:\n")
    print(interpretation)
