"""Connettore "web": scarica una pagina, estrae il testo e calcola l'hash.

Restituisce un Snapshot con testo normalizzato e hash SHA-256. Se il server
risponde 304 (ETag / Last-Modified invariati) lo snapshot e' None: nessuna
modifica da controllare.
"""

from __future__ import annotations

import hashlib
import re
from dataclasses import dataclass

import requests
from bs4 import BeautifulSoup

USER_AGENT = "CollettoreNotifiche/1.0 (+https://github.com/)"
TIMEOUT_S = 20


@dataclass
class Snapshot:
    text: str
    sha256: str
    etag: str | None
    last_modified: str | None


def extract_text(html: str, selector: str | None) -> str:
    soup = BeautifulSoup(html, "html.parser")
    for tag in soup(["script", "style", "noscript", "template"]):
        tag.decompose()

    if selector:
        nodes = soup.select(selector)
        if not nodes:
            # Titolo e dimensione aiutano a capire se il sito ha restituito un'altra pagina.
            title = soup.title.get_text(strip=True) if soup.title else None
            raise ValueError(
                f"Selettore non trovato: {selector!r} "
                f"(titolo pagina: {title!r}, {len(html)} caratteri)"
            )
        raw = "\n".join(node.get_text(" ", strip=True) for node in nodes)
    else:
        raw = soup.get_text(" ", strip=True)

    # Normalizza spazi e a capo per evitare falsi positivi.
    return re.sub(r"\s+", " ", raw).strip()


def fetch(url: str, selector: str | None, state: dict) -> Snapshot | None:
    """Scarica la pagina. `state` contiene etag/last_modified dell'ultima run."""
    headers = {"User-Agent": USER_AGENT, "Accept-Language": "it,en;q=0.8"}
    if state.get("etag"):
        headers["If-None-Match"] = state["etag"]
    if state.get("last_modified"):
        headers["If-Modified-Since"] = state["last_modified"]

    response = requests.get(url, headers=headers, timeout=TIMEOUT_S)
    if response.status_code == 304:
        return None
    response.raise_for_status()

    text = extract_text(response.text, selector)
    digest = hashlib.sha256(text.encode("utf-8")).hexdigest()
    return Snapshot(
        text=text,
        sha256=digest,
        etag=response.headers.get("ETag"),
        last_modified=response.headers.get("Last-Modified"),
    )
