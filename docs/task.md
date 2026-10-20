## 1. Dependency Upgrade
- [x] Update root `build.gradle.kts` (AGP 8.3.2, Kotlin 1.9.24)
- [x] Update `app/build.gradle.kts` (Compose, AndroidX, security-crypto)

## 2. Security Hardening
- [x] Create `data_extraction_rules.xml`
- [x] Update `AndroidManifest.xml`
- [x] Update `network_security_config.xml` (cert pinning)
- [x] Create `SecurePrefs.kt`
- [x] Migrate `SettingsScreen.kt` to SecurePrefs + fix ArrowBack
- [x] Migrate `StationSelectScreen.kt` to SecurePrefs
- [x] Harden `EavRepository.kt` (timeouts, sanitize)

## 3. Frontend Improvements
- [x] Fix `ScheduleScreen.kt` (AnimatedVisibility, remove unused params)
- [x] Improve `StationSelectScreen.kt` (AnimatedVisibility)

## 4. Build & Proguard
- [x] Update `proguard-rules.pro`
- [x] Build release APK
- [x] Verify APK
