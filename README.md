# DreamGrid

DreamGrid is a creative hybrid project combining Java and Python to analyze dreams, store them, and turn them into symbolic experiences. It features:

* A Java backend with SQLite for dream storage and logic
* A Python-based AI oracle (FlameBot) using Hugging Face models
* REST API with Flask for dream interpretation
* Game-inspired structure for exploring dreams as levels

---

## 📅 Project Structure

* `flamebot.py` – Python class using HuggingFace Transformers to interpret dreams
* `app.py` – Flask server exposing `/analyze` endpoint
* `java/` – Java code for database, dream saving, tagging
* `data/` – SQLite database folder
* `.venv/` – Python virtual environment (not committed)

---

## ⚡ How to Run

### 1. Clone the project

```bash
git clone https://github.com/your-username/dream-grid.git
cd dream-grid
```

### 2. Create virtual environment

```bash
python3 -m venv .venv
source .venv/bin/activate   # For Linux/macOS
# or
.venv\Scripts\activate     # For Windows
```

### 3. Install Python dependencies

```bash
pip install -r requirements.txt
```

### 4. Run the Python Flask API

```bash
python app.py
```

### 5. Run the Java app

```bash
./gradlew run
```

---

## 📂 SQLite DB

The database will be created automatically in `data/dreams.db`. Tables are created at runtime if not existing.

---

## 💡 Usage

* Run the Python Flask server (`app.py`) on port `5005`
* Run the Java app
* Input a dream in the terminal or UI
* Dream will be interpreted by FlameBot and saved in DB

---

## 🌟 Features Coming Soon

* Web/mobile UI
* Dream Tree and Shadow Events
* FlameBot memory and evolving character

---

## Dev Notes

* Make sure `torch` and `transformers` are installed
* No Hugging Face token is needed for `google/flan-t5-base`
* You can update the model or logic by editing `flamebot.py`

---

## Roadmap

* [x] Flask API
* [x] Java DB handler
* [x] Basic AI interpretation
* [ ] Dream UI + interactive grid
* [ ] Full game mechanics

---

Made with ❤️ by Anelia
