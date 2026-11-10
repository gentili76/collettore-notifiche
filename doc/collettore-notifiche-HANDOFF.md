# Handoff: progetto "Collettore Notifiche"

Documento per una nuova chat. Riassume tutto quello che è stato fatto, le
configurazioni, gli account e le risorse create. **Non contiene segreti**: le
chiavi stanno nei percorsi indicati, non qui. Il file sta fuori dal repo di
proposito, quindi non è pubblicato su GitHub.

Data: 2026-11-10. Lingua di lavoro: italiano.

---

## 1. Obiettivo

App Android che raccoglie notifiche da varie fonti. Primo caso d'uso: avvisare
quando il contenuto di una pagina web cambia.

Architettura scelta: il controllo avviene lato server (non sul telefono), per
evitare i limiti di WorkManager/Doze.

```
GitHub Actions (cron ogni 5 min)
  -> worker Python: scarica pagine, confronta hash con lo stato precedente
  -> se cambia: messaggio data-only FCM al topic "events"
  -> App Android: riceve, salva in Room, mostra notifica
```

Roadmap: prototipo su GitHub Actions + FCM (fatto). Poi, versione stabile su
VM Oracle AMD Micro o VPS (~4 €/mese) con FastAPI + SQLite. Il codice del
connettore resta lo stesso.

---

## 2. Stato attuale

| Parte | Stato |
|---|---|
| Repo GitHub pubblico | Creato, `main` allineato, ultimo commit `1b598f4` |
| Worker Python | Scritto, eseguito dal workflow (primo run riuscito) |
| Workflow GitHub Actions | Attivo (cron ogni 5 min + avvio manuale), run riuscito |
| Secret GitHub | Impostati: `FCM_SERVICE_ACCOUNT`, `FCM_PROJECT_ID` |
| Progetto Firebase | Creato e configurato |
| Service account FCM | Creato, ruolo assegnato, chiave locale |
| App Android | Scritta, `assembleDebug` riuscito (APK debug generato) |
| Invio FCM | Messaggio di test accettato da Firebase (HTTP 2xx) |
| Test sul telefono | **NON fatto**: nessun dispositivo collegato, nessuna notifica ricevuta |

---

## 3. Percorsi locali

| Cosa | Percorso |
|---|---|
| Repo progetto (codice) | `/Users/lucagentili/workspace/collettore-notifiche` |
| Questo file | `/Users/lucagentili/workspace/collettore-notifiche-HANDOFF.md` |
| Piano approvato | `/Users/lucagentili/.claude/plans/ide-context-active-document-users-lucag-glittery-cookie.md` |
| Chiave service account FCM | `~/.config/collettore-notifiche/service-account.json` (permessi 600) |
| `google-services.json` | `~/.config/collettore-notifiche/google-services.json` (copiato anche in `app/`, ignorato da git) |
| APK debug | `app/build/outputs/apk/debug/app-debug.apk` |
| Progetto Android Studio (contesto precedente) | `/Users/lucagentili/workspace/contradasanbiagioorte` (NON modificato in questo lavoro) |

File temporanei creati (si possono cancellare): `/tmp/cn-android` (copia del
template Android), `/tmp/cn-build.log`, `/tmp/cn-project-id`.

---

## 4. Account, accessi e link

| Servizio | Account | Dettaglio |
|---|---|---|
| GitHub | `gentili76` | Login con `gh auth login --web`, poi aggiunto scope `workflow` con `gh auth refresh -s workflow`. Token nel keyring di macOS. |
| Google Cloud / gcloud | `gentili76@gmail.com` | Già loggato. **Progetto attivo impostato su `collettore-notifiche-pdmhm`** (config globale di gcloud). |
| Firebase CLI | `gentili76@gmail.com` | Già loggato (`firebase login:list`). |
| Homebrew | — | Installato `gh` 2.103.3 (`brew install gh`). |

Link utili:
- Repo: https://github.com/gentili76/collettore-notifiche
- Actions: https://github.com/gentili76/collettore-notifiche/actions
- Run riuscito: https://github.com/gentili76/collettore-notifiche/actions/runs/42836467406
- Secret del repo: https://github.com/gentili76/collettore-notifiche/settings/secrets/actions
- Console Firebase: https://console.firebase.google.com/project/collettore-notifiche-pdmhm/overview

---

## 5. Risorse create su Google Cloud / Firebase

- **Progetto Firebase**: `collettore-notifiche-pdmhm`, display name "Collettore Notifiche", numero progetto non annotato (visibile in console).
- **App Android registrata**:
  - package: `it.lucagentili.collettore`
  - app ID: `1:741253291359:android:8b72943dd58b754631eaea`
- **Service account**: `fcm-sender@collettore-notifiche-pdmhm.iam.gserviceaccount.com`
  - ruolo: `roles/firebasecloudmessaging.admin` (il ruolo `roles/firebasemessaging.admin` è stato rifiutato: non valido per questo progetto)
  - chiave JSON creata e salvata in `~/.config/collettore-notifiche/service-account.json`
- **API**: `fcm.googleapis.com` abilitata. Abilitazione di `firebasecloudmessaging.googleapis.com` fallita con `SERVICE_CONFIG_NOT_FOUND_OR_PERMISSION_DENIED`, ma l'invio FCM funziona lo stesso.
- **Progetto Firebase già esistente nell'account, NON toccato**: `ContradaSanBiagioOrte` (`contradasanbiagioorte-c676b`), `OrtanaApp` (`ortanaapp`).

---

## 6. Repo GitHub e secret

- Repo creato con `gh repo create collettore-notifiche --public --source . --remote origin --push`. Il push iniziale è stato **rifiutato** per mancanza dello scope `workflow`; risolto con `gh auth refresh -s workflow`.
- Il push successivo ha usato `gh` come credential helper **solo per quel comando** (`git -c credential.helper= -c 'credential.helper=!gh auth git-credential' push`). **La config globale di git non è stata modificata.** Per i push futuri lo stesso comando funziona.
- Secret impostati con `gh secret set`:
  - `FCM_SERVICE_ACCOUNT` (contenuto del file JSON della chiave)
  - `FCM_PROJECT_ID` = `collettore-notifiche-pdmhm`
- Il primo run del workflow ha committato `state/esempio.json` come utente `github-actions[bot]`. Ho già fatto il pull in locale.

Storia dei commit su `main`:
1. `64ada43` Prototipo: worker Python, workflow GitHub Actions, sorgenti di esempio
2. `6a7f643` chore: aggiorna stato sorgenti (bot)
3. `1b598f4` App Android: FCM, storico Room, notifiche, feed; worker invia messaggi data-only

---

## 7. Struttura del repo

```
collettore-notifiche/
  .github/workflows/check.yml     # cron */5 + workflow_dispatch, committa state/
  .gitignore                      # esclude google-services.json, local.properties, service-account*.json, build/
  README.md                       # configurazione sorgenti, secret, test locale
  sources.json                    # sorgenti da controllare (ora: solo example.com, DA SOSTITUIRE)
  state/                          # stato per sorgente, aggiornato dal bot
  worker/
    check.py                      # entrypoint: legge sources.json, confronta hash, invia FCM
    connectors/web.py             # GET con ETag/If-Modified-Since, selettore CSS, SHA-256
    connectors/__init__.py
    fcm.py                        # invio FCM HTTP v1 via service account, messaggio DATA-ONLY
    requirements.txt              # requests, beautifulsoup4, google-auth
  build.gradle.kts, settings.gradle.kts, gradle.properties, gradlew(.bat)
  gradle/libs.versions.toml       # version catalog
  gradle/wrapper/                 # Gradle 9.1.0
  app/
    build.gradle.kts              # namespace/app id it.lucagentili.collettore, minSdk 26, targetSdk/compileSdk 37
    google-services.json          # NON in git
    proguard-rules.pro
    src/main/AndroidManifest.xml  # POST_NOTIFICATIONS, MainActivity, CollettoreMessagingService
    src/main/kotlin/it/lucagentili/collettore/
      MainActivity.kt             # subscribeToTopic("events"), richiesta permesso notifiche, FeedScreen
      data/EventEntity.kt         # tabella Room "events"
      data/EventDao.kt            # insert, observeAll (Flow)
      data/AppDatabase.kt         # DB singleton "collettore.db"
      messaging/CollettoreMessagingService.kt  # onMessageReceived: notifica + salva
      messaging/FcmTopics.kt      # EVENTS_TOPIC = "events"
      notifications/Notifications.kt  # canale "eventi", notifica con tap -> URL
      ui/feed/FeedScreen.kt, FeedViewModel.kt  # lista eventi, stato vuoto
      ui/theme/                   # tema del template
```

Contratto del messaggio (data-only, chiavi tutte stringhe):
`source_id`, `title`, `body`, `url`, `ts` (epoch secondi).
Il worker lo costruisce in `check.py` (`send_event`), l'app lo legge in
`CollettoreMessagingService.onMessageReceived`.

---

## 8. Configurazione del worker

- Sorgenti in `sources.json`: `id`, `name`, `url`, `selector` (CSS, opzionale; `null` = tutto il testo), `interval_min`.
- Prima esecuzione di una sorgente: salva lo snapshot, **nessuna notifica**.
- Esecuzioni successive: hash SHA-256 del testo normalizzato. Se cambia, invia un evento con un breve diff (`vecchio → nuovo`, max 200 caratteri).
- Stato in `state/<id>.json`: `sha256`, `text`, `etag`, `last_modified`, `checked_at`.
- Errore su una sorgente: loggato, non blocca le altre; il workflow esce con codice 1 se ci sono errori.
- Test locale: `python worker/check.py --force` con `FCM_PROJECT_ID` e `FCM_SERVICE_ACCOUNT` impostati (vedi `README.md`).

---

## 9. Configurazione Android

- Versioni (da `gradle/libs.versions.toml`): AGP 9.0.1, Kotlin 2.3.20, Compose BOM 2026.03.01, Firebase BoM 35.1.0, google-services 4.5.0, Room 2.8.5, **KSP 2.3.13** (la versione `1.5.30-1.0.0` non andava: incompatibile con Kotlin 2.3.20).
- Template generato con `android create empty-activity` (Android CLI, incluso in Android Studio). Poi rimossi i file di esempio e la dipendenza Navigation3.
- Build: `./gradlew :app:assembleDebug` con `JAVA_HOME` = JDK di Android Studio (`/Applications/Android Studio.app/Contents/jbr/Contents/Home`). Il JDK di sistema non c'è. **Nessun JAVA_HOME è stato scritto nei file del progetto.**
- `minSdk 26`: perché il canale notifiche serve da Android 8.
- Permesso `POST_NOTIFICATIONS` chiesto a runtime su Android 13+.

---

## 10. Test già fatti

- `py_compile` e test di `extract_text` / `short_diff` con dipendenze reali (venv temporaneo, poi cancellato).
- Run GitHub Actions manuale: `check-sources` riuscito, log `primo snapshot salvato, nessuna notifica` per `esempio`.
- Invio FCM di test al topic `events` con evento fittizio (`title: Test collettore`): accettato da Firebase. Nessun dispositivo era iscritto, quindi nessuna notifica è arrivata.
- `./gradlew :app:assembleDebug`: BUILD SUCCESSFUL.

---

## 11. Cosa NON è stato fatto / prossimi passi

1. **Installare l'app su un telefono o emulatore** e verificare: permesso notifiche, iscrizione al topic (avviene all'avvio), ricezione di una notifica di test dal topic, salvataggio nel feed.
2. **Test end-to-end reale**: sostituire `example.com` in `sources.json` con una pagina che cambia (o modificare la pagina di test), attendere il workflow e verificare la push sul telefono.
3. **Schermata sorgenti in sola lettura** (prevista nel piano, non scritta).
4. **Test automatici**: il template li aveva, sono stati rimossi con il codice di esempio. Nessun test nuovo.
5. **Gestione token / multi-dispositivo**: oggi tutti gli iscritti al topic ricevono tutto. Va bene per un solo utente.
6. **Repo pubblico**: `state/` contiene il testo delle pagine controllate, visibile a tutti. Per pagine private serve la VM/VPS (roadmap fase 2).
7. **Versione stabile** su VM Oracle AMD Micro (o VPS) con FastAPI + SQLite: da fare.
8. **Commit del `.gitignore`/README**: già nel repo; nessun file con segreti tracciato (verificato con `git check-ignore`).

Note operative:
- Il workflow gira ogni 5 minuti anche senza modifiche: consuma minuti Actions (gratis per repo pubblico).
- Per cambiare sorgenti: modificare `sources.json`, commit e push.
- Per rigenerare la chiave FCM: creare una nuova chiave per `fcm-sender@...`, aggiornare il secret `FCM_SERVICE_ACCOUNT` e il file locale.

---

## 12. Comandi utili

```bash
# Build app
cd /Users/lucagentili/workspace/collettore-notifiche
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:assembleDebug

# Push (con gh come credential helper, solo per il comando)
git -c credential.helper= -c 'credential.helper=!gh auth git-credential' push origin main

# Lancia il workflow a mano e controlla
gh workflow run check-sources
gh run list --workflow check-sources --limit 3

# Stato sorgenti
cat state/esempio.json   # oppure aprire il file
```

---

## 13. Cose da sapere se tocchi l'account

- `gcloud` è ora configurato sul progetto `collettore-notifiche-pdmhm` (`gcloud config get-value project`). Per tornare al precedente: `gcloud config set project contradasanbiagioorte-c676b`.
- Nessuna risorsa in `ContradaSanBiagioOrte` o `OrtanaApp` è stata modificata.
- La chiave service account è un segreto: non va in chat, né in git, né in screenshot.
