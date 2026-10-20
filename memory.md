# Personal & Project Memory — EavOrari

Questo file documenta le memorie di sistema, l'ambiente di build e le regole specifiche del progetto per garantire stabilità e consistenza nelle future sessioni di sviluppo.

## Personal Global Memory
- Usa sempre `sudo` per aggiornare Gemini CLI: `npm install -g @google/gemini-cli`.
- Usa sempre `sudo` per aggiornare opencode: `opencode upgrade`.
- Alias consigliati:
  ```sh
  alias gemini-update='sudo npm install -g @google/gemini-cli'
  alias opencode-update='sudo opencode upgrade'
  ```

## Ambiente di Build & Vincoli
- **Java Home:** La compilazione richiede obbligatoriamente `JAVA_HOME=/tmp/opencode/jdk`.
- **Comandi di Build:**
  - Release APK (con ottimizzazione R8 e firma):
    ```sh
    export JAVA_HOME=/tmp/opencode/jdk && ./gradlew assembleRelease
    ```
  - Debug APK:
    ```sh
    export JAVA_HOME=/tmp/opencode/jdk && ./gradlew assembleDebug
    ```
- **Firmare APK:** La keystore di firma si trova in `/home/michde/orari/release.keystore` (alias: `eavorari`, password: `EavOrari2026!`).

## Sicurezza & Play Protect 2026
- **Target SDK 35 (Android 15):** Compila con compileSdk 35 e targetSdk 35.
- **Dati Cifrati (`SecurePrefs`):** Tutte le letture/scritture delle preferenze devono transitare da `com.eavorari.data.SecurePrefs.get(context)`. Non usare preferenze in chiaro. Il file cifrato è `eav_orari_secure`.
- **Esclusione Backup:** I dati locali sono protetti in `data_extraction_rules.xml` che blocca completamente i backup sul cloud e i trasferimenti fisici dei file delle SharedPreferences cifrate.
- **Rete & SSL Config:** Il file `network_security_config.xml` inibisce il traffico non cifrato e definisce regole esplicite HTTPS per `eavsrl.it` e subdomini.
- **Sanitizzazione:** I dati estratti tramite Jsoup dalle API o pagine web pubbliche devono sempre essere ripuliti con `Jsoup.clean(html, Safelist.relaxed())` prima del parsing.

## Design Patterns & UI
- **Touch Target:** Mantieni sempre aree di tocco minime di 48–50dp.
- **Padding di Sistema:** Assicurati che i container principali abbiano `statusBarsPadding()` e `navigationBarsPadding()`.
- **Bordi Card:** Mantieni bordi coerenti di `0.5.dp` con colore `Divider.copy(alpha = 0.4f)` su sfondo `Card` per dare un aspetto premium.
- **Animazioni List:** Usa `animateItemPlacement()` combinato con chiavi univoche (`key`) sui `LazyColumn` per gestire le transizioni fluide durante i filtri di testo delle stazioni.
- **Gesture Predittiva Indietro:** Gestisci il tasto indietro con `BackHandler` esplicito per pulire i filtri attivi prima di causare la chiusura o la navigazione fuori dalla schermata.
