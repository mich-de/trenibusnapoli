# Hardening App: Codice, Frontend & Play Protect 2026

Aggiornamento completo dell'app per renderla sicura, moderna e compatibile con Play Protect 2026.

## Proposed Changes

### 1. Dependency Upgrade (Sicurezza core)

#### [MODIFY] [build.gradle.kts](file:///home/michde/orari/eav-orari/build.gradle.kts)
- AGP `8.2.2` → `8.3.2` (massimo per Gradle 8.5)
- Kotlin `1.9.22` → `1.9.24`

#### [MODIFY] [app/build.gradle.kts](file:///home/michde/orari/eav-orari/app/build.gradle.kts)
- `compileSdk` / `targetSdk` = `35` (API 35, Android 15) — ✅ già fatto
- Compose Compiler `1.5.9` → `1.5.14` (allineato a Kotlin 1.9.24)
- Compose BOM `2024.02.00` → `2024.06.00` (ultimo stabile per questa catena)
- AndroidX Activity Compose `1.8.2` → `1.9.3`
- AndroidX Navigation `2.7.7` → `2.8.4`
- AndroidX Lifecycle `2.7.0` → `2.8.7`
- Kotlinx Coroutines `1.8.0` → `1.9.0`
- Jsoup `1.17.2` → `1.18.3`
- Aggiunta `androidx.security:security-crypto:1.1.0-alpha06` per **EncryptedSharedPreferences**

---

### 2. Security Hardening (Play Protect 2026)

#### [MODIFY] [AndroidManifest.xml](file:///home/michde/orari/eav-orari/app/src/main/AndroidManifest.xml)
- Aggiunta `android:hasFragileUserData="false"` (dichiara che l'app non contiene dati utente sensibili)
- Aggiunta `android:dataExtractionRules` per Android 12+ (obbligatorio con targetSdk 35, sostituisce `allowBackup`)
- `android:enableOnBackInvokedCallback="true"` (predictive back obbligatorio per targetSdk 35+)

#### [NEW] [data_extraction_rules.xml](file:///home/michde/orari/eav-orari/app/src/main/res/xml/data_extraction_rules.xml)
- Blocca completamente il backup cloud e device-to-device dei dati dell'app

#### [MODIFY] [network_security_config.xml](file:///home/michde/orari/eav-orari/app/src/main/res/xml/network_security_config.xml)
- Certificate pinning per i domini EAV API (`orariotreni.eavsrl.it`, `www.eavsrl.it`)

#### [NEW] [SecurePrefs.kt](file:///home/michde/orari/eav-orari/app/src/main/java/com/eavorari/data/SecurePrefs.kt)
- Wrapper per `EncryptedSharedPreferences` che sostituisce `getSharedPreferences("eav_orari",...)`
- Chiavi utente cifrate con AES256_SIV + AES256_GCM

#### [MODIFY] [SettingsScreen.kt](file:///home/michde/orari/eav-orari/app/src/main/java/com/eavorari/ui/screens/SettingsScreen.kt)
- Migrazione da `context.getSharedPreferences` → `SecurePrefs.get(context)`
- Fix warning `Icons.Default.ArrowBack` → `Icons.AutoMirrored.Filled.ArrowBack`

#### [MODIFY] [StationSelectScreen.kt](file:///home/michde/orari/eav-orari/app/src/main/java/com/eavorari/ui/screens/StationSelectScreen.kt)
- Migrazione da `context.getSharedPreferences` → `SecurePrefs.get(context)`

#### [MODIFY] [EavRepository.kt](file:///home/michde/orari/eav-orari/app/src/main/java/com/eavorari/data/EavRepository.kt)
- Timeout di connessione espliciti (connect 10s, read 15s)
- Sanitizzazione input HTML con `Jsoup.clean()` prima del parsing degli scioperi

---

### 3. Frontend Quality Improvements

#### [MODIFY] [ScheduleScreen.kt](file:///home/michde/orari/eav-orari/app/src/main/java/com/eavorari/ui/screens/ScheduleScreen.kt)
- Predictive back gesture support tramite `BackHandler` esplicito
- Aggiunta `AnimatedVisibility` per le transizioni filtro destinazione
- Aggiunta bordo sottile alle righe del treno per dare profondità visiva
- Fix: rimozione parametri `allTrains` e `tipoLabel` inutilizzati (warning del compilatore)

#### [MODIFY] [StationSelectScreen.kt](file:///home/michde/orari/eav-orari/app/src/main/java/com/eavorari/ui/screens/StationSelectScreen.kt)
- Aggiunta `AnimatedVisibility` per la comparsa della lista stazioni filtrate

#### [MODIFY] [SettingsScreen.kt](file:///home/michde/orari/eav-orari/app/src/main/java/com/eavorari/ui/screens/SettingsScreen.kt)
- Aggiunta bordi consistenti come nelle altre schermate
- Fix deprecazione `ArrowBack`

---

### 4. Build & Proguard

#### [MODIFY] [proguard-rules.pro](file:///home/michde/orari/eav-orari/app/proguard-rules.pro)
- Aggiunta keep rules per `androidx.security.crypto.**`
- Aggiunta `-assumenosideeffects` per rimuovere i log in release

## Verification Plan

### Automated Tests
- `export JAVA_HOME=/tmp/opencode/jdk && ./gradlew assembleRelease` — deve compilare con 0 errori
- Verifica dimensione APK finale (prevista ~1.5 MB grazie a R8)
