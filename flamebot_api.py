from fastapi import FastAPI, Request
from pydantic import BaseModel
from flamebot_local import FlameBot

app = FastAPI()
bot = FlameBot()

class DreamRequest(BaseModel):
    symbols: list[str]

@app.post("/interpret")
async def interpret_dream(req: DreamRequest):
    interpretation = bot.interpret(req.symbols)
    return {"interpretation": interpretation}