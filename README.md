# Viki Web per Fire TV (non ufficiale)

Piccola app Android che apre viki.com in una WebView e la rende usabile con il
telecomando della Fire Stick tramite un puntatore a schermo. Non è un'app
ufficiale di Viki/Rakuten.

## Compilare l'APK
1. Installa Android Studio (versione recente) e apri la cartella `FireViki`.
2. Lascia che Android Studio scarichi Gradle e gli SDK richiesti (compileSdk 34).
3. Menu **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
4. L'APK si trova in `app/build/outputs/apk/debug/app-debug.apk`
   (è firmato con la chiave di debug, quindi installabile).

## Installare sulla Fire Stick
- **Con adb** (Impostazioni → My Fire TV → Opzioni sviluppatore: attiva
  "Debug ADB" e "App da fonti sconosciute"; l'IP è in Info → Rete):
  ```
  adb connect IP_DELLA_FIRE_STICK
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  ```
- **Con Downloader**: carica l'APK su un tuo spazio cloud e scaricalo dall'app
  Downloader, poi installa.

L'app compare tra le app installate come "Viki Web".

## Telecomando
| Tasto | Azione |
|---|---|
| Frecce | muovono il puntatore; ai bordi scorrono la pagina |
| Selezione | clic |
| Indietro | pagina precedente / esci da schermo intero / esci dall'app |
| Menu (≡) | torna alla home di Viki |
| Play/Pausa | play/pausa del video |
| Avanti/Indietro veloce | ±10 secondi |
| Schermo intero: ◀ ▶ / Selezione | ∓10 s / play-pausa |

## Limiti noti
- La WebView di Fire OS 6 usa un Chromium datato: il login di Viki
  (captcha, accessi social) o la riproduzione protetta da DRM potrebbero non
  funzionare. Per il login usa email e password, non Google/Facebook/Apple.
- Se i video non partono, il problema è il motore web del dispositivo e
  non il codice dell'app.
