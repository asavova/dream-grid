import json
from pathlib import Path
from typing import Any, Dict


class RuleLoadError(RuntimeError):
    pass


def load_rule_file(path: Path, required_fields: list[str]) -> Dict[str, Any]:
    try:
        with Path(path).open("r", encoding="utf-8") as file:
            rules = json.load(file)
    except FileNotFoundError as exc:
        raise RuleLoadError(f"Rule file not found: {path}") from exc
    except json.JSONDecodeError as exc:
        raise RuleLoadError(f"Rule file is malformed JSON: {path}: {exc.msg}") from exc

    if not isinstance(rules, dict):
        raise RuleLoadError(f"Rule file must contain a JSON object: {path}")

    missing = [field for field in required_fields if field not in rules]
    if missing:
        raise RuleLoadError(f"Rule file {path} is missing required fields: {', '.join(missing)}")

    return rules


def load_interpretation_rules(path: Path) -> Dict[str, Any]:
    rules = load_rule_file(path, ["symbols", "defaultThemes", "defaultSummary"])
    if not isinstance(rules["symbols"], list):
        raise RuleLoadError("dream interpretation rules field 'symbols' must be a list")
    return rules


def load_content_safety_rules(path: Path) -> Dict[str, Any]:
    rules = load_rule_file(path, ["categories"])
    if not isinstance(rules["categories"], list):
        raise RuleLoadError("content safety rules field 'categories' must be a list")
    return rules


def load_classification_rules(path: Path) -> Dict[str, Any]:
    rules = load_rule_file(path, ["classifications"])
    if not isinstance(rules["classifications"], list):
        raise RuleLoadError("classification rules field 'classifications' must be a list")
    return rules
