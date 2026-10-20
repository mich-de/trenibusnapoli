# Trasporti Penisola Sorrentina

Applicazione Android premium e moderna per visualizzare in tempo reale gli orari dei treni EAV (Circumvesuviana) della Penisola Sorrentina, gli scioperi e altre linee di trasporto (bus, metropolitane, ecc.).

## APK di Release Pronto all'Uso
La release compilata, ottimizzata e firmata è disponibile direttamente in questa repository:
- **[Scarica APK (app-release.apk)](./app-release.apk)** (~1.5 MB)

L'APK è stato minificato ed ottimizzato tramite R8/Proguard (passando da ~11MB a soli ~1.5MB) e risulta pienamente conforme agli standard di sicurezza e privacy richiesti da **Google Play Protect 2026**.

## Caratteristiche Principali
- **Aesthetic Premium Retro-Terminal:** Palette OLED scura con elementi a scorrimento, scritte arancio/ambra e animazioni a split-flap.
- **Dati Real-time:** Orari aggiornati dei treni direttamente dai canali EAV e monitoraggio automatico dello stato degli scioperi.
- **Sicurezza Avanzata (Play Protect 2026):**
  - **Cifratura Locale:** Preferenze salvate in modo sicuro tramite `EncryptedSharedPreferences` (algoritmi AES256-SIV/GCM).
  - **Prevenzione Data Leak:** Configurate regole rigide in `data_extraction_rules.xml` che bloccano il backup automatico o il trasferimento dei dati dell'app a sorgenti esterne.
  - **Network Config Hardening:** Disabilitato il traffico non cifrato (Cleartext traffic) e configurati domini attendibili espliciti.
  - **Sanitizzazione Dati:** Sanitizzazione dei contenuti HTML provenienti dalle API pubbliche per prevenire exploit/injection.
  - **Target SDK 35 (Android 15):** Pieno allineamento agli standard moderni con supporto nativo alle gesture predittive di indietro (`BackHandler` per deselezionare i filtri di destinazione prima di uscire).

## Requisiti di Compilazione
L'applicazione richiede Java 17 e l'Android SDK installati.
Per compilare l'APK da riga di comando:

```sh
# Imposta la variabile JAVA_HOME ed esegui il build release
export JAVA_HOME=/tmp/opencode/jdk && ./gradlew assembleRelease
```

L'APK finale sarà generato in `app/build/outputs/apk/release/app-release.apk`.
