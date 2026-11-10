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

## Pannello web per le sorgenti

`panel/` contiene una pagina statica (GitHub Pages) per modificare `sources.json`
dal browser. Salva il file direttamente nel repo tramite l'API GitHub, quindi il
prossimo controllo usa le nuove sorgenti.

Prima configurazione:

1. Settings → Pages → Source: **GitHub Actions** (il workflow `pages` pubblica `panel/`).
2. Crea un token a grana fine su GitHub (Settings → Developer settings → Fine-grained tokens),
   limitato al solo repo `collettore-notifiche`, con permesso **Contents: Read and write**.
3. Apri la pagina del pannello, incolla il token e premi "Salva token". Il token resta solo nel
   localStorage di quel browser e non viene mai inviato a servizi diversi da `api.github.com`.

La pagina è pubblica, ma senza token non può scrivere nulla.

Attenzione: cambiare l'`id` di una sorgente fa ripartire il suo stato, quindi il primo
controllo successivo salva uno snapshot nuovo senza notificare.

## Test locale

```bash
pip install -r worker/requirements.txt
export FCM_PROJECT_ID=...
export FCM_SERVICE_ACCOUNT="$(cat service-account.json)"
python worker/check.py --force
```

La prima esecuzione di una sorgente salva solo lo snapshot, senza notificare.
Dalla seconda, una modifica del contenuto produce una notifica.
