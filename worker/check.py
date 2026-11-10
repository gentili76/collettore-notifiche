"""Controlla le sorgenti scadute, rileva cambiamenti e invia push FCM.

Uso:
  python worker/check.py            # esegue i controlli dovuti
  python worker/check.py --force    # ignora l'intervallo (test)

Stato per sorgente in state/<id>.json:
  {"sha256": "...", "etag": "...", "last_modified": "...", "checked_at": <epoch>}
"""

from __future__ import annotations

import difflib
import json
import sys
import time
from pathlib import Path

from connectors import web
from fcm import send_event

ROOT = Path(__file__).resolve().parent.parent
SOURCES_FILE = ROOT / "sources.json"
STATE_DIR = ROOT / "state"
MAX_BODY_CHARS = 200


def load_json(path: Path, default):
    if not path.exists():
        return default
    return json.loads(path.read_text(encoding="utf-8"))


def save_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def is_due(state: dict, interval_min: int, now: float, force: bool) -> bool:
    if force:
        return True
    return now - state.get("checked_at", 0) >= interval_min * 60


def short_diff(old: str, new: str) -> str:
    """Breve riassunto testuale della differenza, per il corpo della notifica."""
    old_words, new_words = old.split(), new.split()
    matcher = difflib.SequenceMatcher(a=old_words, b=new_words, autojunk=False)
    parts = []
    for tag, i1, i2, j1, j2 in matcher.get_opcodes():
        if tag == "equal":
            continue
        removed = " ".join(old_words[i1:i2])
        added = " ".join(new_words[j1:j2])
        if removed and added:
            parts.append(f"{removed} → {added}")
        elif added:
            parts.append(f"+ {added}")
        elif removed:
            parts.append(f"- {removed}")
    summary = " | ".join(parts) or "contenuto modificato"
    return summary[:MAX_BODY_CHARS]


def check_source(source: dict, state: dict, now: float) -> dict:
    source_id = source["id"]
    snapshot = web.fetch(source["url"], source.get("selector"), state)

    if snapshot is None:
        print(f"[{source_id}] non modificata (304)")
        state["checked_at"] = now
        return state

    previous_hash = state.get("sha256")
    previous_text = state.get("text", "")

    if previous_hash is None:
        print(f"[{source_id}] primo snapshot salvato, nessuna notifica")
    elif snapshot.sha256 != previous_hash:
        print(f"[{source_id}] cambiamento rilevato")
        send_event(
            {
                "source_id": source_id,
                "title": source.get("name", source_id),
                "body": short_diff(previous_text, snapshot.text),
                "url": source["url"],
                "ts": int(now),
            }
        )
    else:
        print(f"[{source_id}] invariata")

    return {
        "sha256": snapshot.sha256,
        "text": snapshot.text,
        "etag": snapshot.etag,
        "last_modified": snapshot.last_modified,
        "checked_at": now,
    }


def main() -> int:
    force = "--force" in sys.argv
    sources = load_json(SOURCES_FILE, [])
    now = time.time()

    for source in sources:
        state_path = STATE_DIR / f"{source['id']}.json"
        state = load_json(state_path, {})

        if not is_due(state, source.get("interval_min", 15), now, force):
            continue

        try:
            new_state = check_source(source, state, now)
        except Exception as exc:  # una sorgente rotta non blocca le altre
            # ::warning:: crea un'annotazione sul run senza farlo fallire.
            print(f"::warning::[{source['id']}] ERRORE: {exc}")
            continue

        save_json(state_path, new_state)

    return 0


if __name__ == "__main__":
    sys.exit(main())
