import os
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM
import torch

class FlameBot:
    token = os.getenv("HUGGINGFACE_TOKEN")

    tokenizer = AutoTokenizer.from_pretrained("google/flan-t5-small", token=token)
    model = AutoModelForSeq2SeqLM.from_pretrained("google/flan-t5-small", token=token)

    def analyze(self, dream_text: str) -> str:
        prompt = f"Interpret this dream symbolically and personally:\n{dream_text}"
        inputs = self.tokenizer(prompt, add_special_tokens=False, return_tensors="pt", truncation=True)

        with torch.no_grad():
            outputs = self.model.generate(**inputs, max_length=256)

        response = self.tokenizer.decode(outputs[0], skip_special_tokens=True)
        return response
