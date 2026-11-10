# Collettore di notifiche

Il server controlla le sorgenti (pagine web) e invia una push FCM all'app Android
quando il contenuto cambia.

## Struttura

- `worker/` — controllo sorgenti in Python (`check.py`, connettore `connectors/web.py`, invio `fcm.py`)
- `sources.json` — elenco delle sorgenti da controllare
- `state/` — stato per sorgente (hash, ETag), aggiornato automaticamente dal workflow
- `.github/workflows/check.yml` — esecuzione ogni 5 minuti su GitHub Actions
- `app/` — app Android (da creare, richiede `google-services.json`)

## Configurazione sorgenti

Modifica `sources.json`:

```json
{
  "id": "identificativo-unico",
  "name": "Nome mostrato nella notifica",
  "url": "https://...",
  "selector": "div.prezzo",
  "interval_min": 15
}
```

`selector` è opzionale: se `null` viene usato tutto il testo della pagina.
Usare un selettore CSS riduce i falsi positivi (pubblicità, contatori, orari).

## Segreti GitHub (Settings → Secrets and variables → Actions)

- `FCM_SERVICE_ACCOUNT` — JSON del service account Firebase con permessi di invio FCM
- `FCM_PROJECT_ID` — ID del progetto Firebase

## Test locale

```bash
pip install -r worker/requirements.txt
export FCM_PROJECT_ID=...
export FCM_SERVICE_ACCOUNT="$(cat service-account.json)"
python worker/check.py --force
```

La prima esecuzione di una sorgente salva solo lo snapshot, senza notificare.
Dalla seconda, una modifica del contenuto produce una notifica.
