# Collettore di notifiche

Il server controlla alcune pagine web (le **sorgenti**) e, quando il contenuto cambia,
invia una push FCM all'app Android. L'app salva ogni evento in un feed locale, diviso in
**Nuove** e **Viste**.

## Link rapidi

| Cosa | Link |
|---|---|
| Repo GitHub | https://github.com/gentili76/collettore-notifiche |
| Esecuzioni del worker (Actions) | https://github.com/gentili76/collettore-notifiche/actions/workflows/check.yml |
| Esecuzioni pagina pannello (Pages) | https://github.com/gentili76/collettore-notifiche/actions/workflows/pages.yml |
| Pannello web sorgenti | https://gentili76.github.io/collettore-notifiche/ |
| Pagina di test | https://gentili76.github.io/collettore-notifiche/test.html |
| Segreti del repo | https://github.com/gentili76/collettore-notifiche/settings/secrets/actions |
| Token GitHub (fine-grained) | https://github.com/settings/personal-access-tokens |
| Console Firebase | https://console.firebase.google.com/project/collettore-notifiche-pdmhm/overview |
| Firebase App Distribution | https://console.firebase.google.com/project/collettore-notifiche-pdmhm/appdistribution |
| Scheduler esterno (cron-job.org) | https://cron-job.org |

## Architettura

```
cron-job.org (ogni 5 min)
  -> API GitHub: workflow_dispatch di check.yml
  -> worker Python: scarica le sorgenti, confronta l'hash con state/
  -> se cambia: push FCM data-only al topic "events"
  -> app Android: salva in Room e mostra la notifica
```

Il workflow `check.yml` ha anche un cron `*/5` su GitHub, ma GitHub non lo ha mai
avviato su questo repo: i controlli partono quindi da cron-job.org.

## Struttura del repo

- `worker/` — controllo sorgenti in Python (`check.py`, `connectors/web.py`, `fcm.py`)
- `sources.json` — sorgenti da controllare
- `state/` — stato per sorgente (hash, testo, ultimo controllo), aggiornato dal bot
- `panel/` — pannello web statico per modificare `sources.json` e pagina di test
- `.github/workflows/check.yml` — esecuzione del worker
- `.github/workflows/pages.yml` — pubblicazione di `panel/` su GitHub Pages
- `app/` — app Android (Kotlin, Compose, Room, FCM)
- `doc/` — handoff del progetto

## Sorgenti

`sources.json` contiene un elenco di oggetti così:

```json
{
  "id": "identificativo-unico",
  "name": "Nome mostrato nella notifica",
  "url": "https://...",
  "selector": "div.prezzo",
  "interval_min": 5
}
```

- `selector` è opzionale: se `null`, si usa tutto il testo della pagina. Un selettore CSS
  riduce i falsi positivi (pubblicità, contatori, orari).
- `interval_min` è l'intervallo minimo tra due controlli della stessa sorgente. Il minimo
  utile è 5 minuti, cioè il passo del cron.
- Cambiare `id` fa ripartire lo stato: il primo controllo salva solo lo snapshot, senza
  notificare.
- La prima esecuzione di una sorgente salva lo snapshot senza notifica; dalla seconda in poi
  una modifica del testo produce una push.
- Se una sorgente fallisce (rete, selettore non trovato), il worker emette un avviso sul run
  e continua con le altre. Il messaggio d'errore include il titolo della pagina ricevuta.

Sorgenti attive: `figc-terni-comunicati` (FIGC Terni), `pagina-test`
(https://gentili76.github.io/collettore-notifiche/test.html), `hortinaeclasses`
(https://www.hortinaeclasses.it/not.html, senza selettore).

## Token e segreti

| Dove | Cosa | Permesso | Usato da |
|---|---|---|---|
| Pannello web | token "collettore-not" | **Contents**: read/write sul solo repo | pannello, per salvare `sources.json` nel repo |
| cron-job.org | token "collettore-notifiche" | **Actions**: read/write sul solo repo | header `Authorization: Bearer ...` del job che avvia il workflow |
| Segreti GitHub | `FCM_SERVICE_ACCOUNT` | service account FCM | worker, per inviare i messaggi |
| Segreti GitHub | `FCM_PROJECT_ID` | ID progetto Firebase (`collettore-notifiche-pdmhm`) | worker |

I valori dei token non vanno mai nel repo né in chat. Il token del pannello non ha scadenza:
se vuoi, rigeneralo con una data e aggiornalo nel pannello. Il token di cron-job.org scade il
30 giugno 2027.

## Pannello web

`panel/index.html` è pubblicata su GitHub Pages dal workflow `pages`. Dalla pagina puoi
aggiungere, modificare ed eliminare sorgenti. Il salvataggio committa `sources.json` su `main`.

Prima configurazione:

1. Settings → Pages → Source: **GitHub Actions**.
2. Crea un token fine-grained con **Contents: Read and write** sul solo repo.
3. Apri il pannello, incolla il token e premi **Salva token**. Il token resta nel
   localStorage di quel browser e viene inviato solo a `api.github.com`.

La pagina è pubblica, ma senza token non può scrivere nulla.

## Scheduler (cron-job.org)

Un job su cron-job.org fa una `POST` ogni 5 minuti a:

`https://api.github.com/repos/gentili76/collettore-notifiche/actions/workflows/check.yml/dispatches`

con body `{"ref":"main"}` e header:

- `Authorization: Bearer <token con Actions: read/write>`
- `Accept: application/vnd.github+json`
- `Content-Type: application/json`

Risposta attesa: `204`.

## App Android

- **Package:** `it.lucagentili.collettore`
- **Firebase app ID:** `1:741253291359:android:8b72943dd58b754631eaea`
- **minSdk 26**, targetSdk/compileSdk 37
- **Icona:** campana bianca su sfondo blu (`res/drawable/ic_launcher_*.xml`)
- **Feed:** tab "Nuove" e "Viste". Un tap sull'evento apre il sito e lo sposta in "Viste".
- **Favicon:** caricata dal servizio Google (`google.com/s2/favicons?domain=...`). Se il sito
  non ha favicon, compare un cerchio con l'iniziale del dominio.
- **Database Room:** versione 2 (migrazione 1→2 aggiunge `seen`).

### Versioning

`versionCode` segue lo schema `MMmmRR`: maggiore, minore, revisione.

- `10100` = 1.1.0 revisione 0
- `10101` = 1.1.0 revisione 1 (`versionName` `1.1.0-r1`)

Per una nuova revisione, incrementa `versionCode` di 1 in `app/build.gradle.kts` e aggiorna
`versionName`.

### Build e distribuzione

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:assembleDebug
```

L'APK debug è in `app/build/outputs/apk/debug/app-debug.apk`. Per la distribuzione di prova
si usa Firebase App Distribution, con il comando `firebase appdistribution:distribute`
(vedi la console).

## Branch

- `main` — branch di produzione. Il workflow `check.yml` e il pannello girano da qui.
- `develop` — sviluppo. Le modifiche passano da qui e vengono portate su `main` quando
  servono in produzione.

## Test locale del worker

```bash
pip install -r worker/requirements.txt
export FCM_PROJECT_ID=...
export FCM_SERVICE_ACCOUNT="$(cat ~/.config/collettore-notifiche/service-account.json)"
python worker/check.py --force
```

`--force` ignora l'intervallo di `interval_min`. Il file della chiave non va mai nel repo
(è escluso da `.gitignore`).
