# flamebot.py

class FlameBot:
    def __init__(self, model_name = "gpt-4o"):
        self.model_name = model_name

    def generate_prompt(self, symbols):
        symbol_list = ", ".join(symbols)
        return f"Interpret a dream with these symbols: {symbol_list}. Use metaphor, poetic logic, and intuitive insight."

    def get_system_message(self):
        return {
            "role": "system",
            "content": (
                "You are FlameBot, an intuitive and poetic dream interpreter AI. "
                "You use symbolic thinking and surreal metaphors. Your answers are lyrical, surprising, and emotionally intelligent."
            )
        }
