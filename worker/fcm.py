"""Invio di notifiche via Firebase Cloud Messaging (HTTP v1) a un topic.

Richiede due variabili d'ambiente:
  FCM_SERVICE_ACCOUNT  JSON del service account (contenuto, non path)
  FCM_PROJECT_ID       ID del progetto Firebase
"""

from __future__ import annotations

import json
import os

import google.auth.transport.requests
from google.oauth2 import service_account

FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging"
TOPIC = "events"


def _access_token() -> str:
    info = json.loads(os.environ["FCM_SERVICE_ACCOUNT"])
    credentials = service_account.Credentials.from_service_account_info(
        info, scopes=[FCM_SCOPE]
    )
    credentials.refresh(google.auth.transport.requests.Request())
    return credentials.token


def send_event(event: dict) -> None:
    """Invia un evento come messaggio al topic.

    `event` deve contenere: title, body, url, source_id, ts.

    Il messaggio e' solo `data` (niente blocco `notification`): cosi' l'app
    riceve sempre l'evento, anche in background, e lo salva nello storico
    prima di mostrare la notifica.
    """
    import requests  # import locale: serve solo quando si invia davvero

    project_id = os.environ["FCM_PROJECT_ID"]
    url = f"https://fcm.googleapis.com/v1/projects/{project_id}/messages:send"
    message = {
        "message": {
            "topic": TOPIC,
            "data": {k: str(v) for k, v in event.items()},
            "android": {"priority": "high"},
        }
    }
    response = requests.post(
        url,
        headers={
            "Authorization": f"Bearer {_access_token()}",
            "Content-Type": "application/json; UTF-8",
        },
        data=json.dumps(message),
        timeout=20,
    )
    response.raise_for_status()
