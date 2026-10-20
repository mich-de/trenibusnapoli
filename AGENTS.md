# AGENTS.md — Trasporti Penisola Sorrentina

## Build

```sh
export JAVA_HOME=/tmp/opencode/jdk && ./gradlew assembleRelease
# debug:
export JAVA_HOME=/tmp/opencode/jdk && ./gradlew assembleDebug
```

Release APK → `app/build/outputs/apk/release/app-release.apk` (~1.5MB, signed & optimized with R8).

Signing: `app/build.gradle.kts` reads keystore at `/home/michde/orari/release.keystore` (alias `eavorari`, password `EavOrari2026!`).

## Architecture

Single-module Android app, Kotlin + Jetpack Compose, **no tests**. No CI/lint config.

| Layer | Key files |
|---|---|
| Nav entry | `MainActivity.kt` — NavHost with 3 routes: `station_select`, `schedule/{stationId}/{tipo}/{stationName}`, `settings` |
| Shared state | `ScheduleViewModel` — MutableStateFlows, loads stations/strikes on init, auto-refresh 30s |
| Data | `EavRepository` — fetches EAV train schedules (POST to `ws_getData.php`), WordPress REST API for strikes, all other data (bus lines, metro, extra operators) is **static/hardcoded** |
| Models | `Models.kt` — Station, TrainInfo, BusLine, BusArea, StrikeInfo, ExtraBusRoute (with `returnStops`), MetroLine, MetroStop, TransportMode enum |
| UI | `StationSelectScreen` (home, 5 tabs), `ScheduleScreen` (departure board), `SettingsScreen`, `RetroComponents` (split-flap, indicator, divider) |
| Theme | `Color.kt` — retro amber-on-dark palette; `FontFamily.Monospace` everywhere |

## Critical gotchas

- **JAVA_HOME** must be set: `/tmp/opencode/jdk` (not in env by default).
- **SharedPreferences** values in Compose: read directly from `SecurePrefs.get(context)` at composition time (do NOT wrap the Preference object in `remember {}` if it needs to reflect changes on navigating back). The underlying storage is encrypted using `EncryptedSharedPreferences` (AES256 SIV/GCM). Prefs file name: `eav_orari_secure`, keys: `station_id`, `station_name`, `tipo` (P/A), `peninsula_enabled`.
- **EAV schedule** is POST (not GET) to `orariotreni.eavsrl.it/teleindicatori/ws_getData.php` with params `device`, `tipoLista`, `codLoc`, `visualizzazione=mobile`.
- **Strikes** cached per date (cache resets at midnight); fetched from WordPress REST API at `wp-json/wp/v2/posts?categories=<linee-vesuviane-id>`.
- **Network**: Enforced HTTPS (`cleartextTrafficPermitted="false"`) via `network_security_config.xml` with explicit trusted domain configs for `eavsrl.it` and subdomains.
- **Bus filtering**: `peninsula_enabled` + saved `station_name` must BOTH be truthy for peninsula-only mode in BUS/NOTTURNI tabs.
- **ExtraBusRoute.returnStops**: return-direction stops differ from outbound (different addresses/piazzas).
- **App name** in `strings.xml`: `app_name` = "Trasporti Penisola Sorrentina".

## Style conventions

- No comments in code.
- Strings in Italian (UI labels).
- `RoundedCornerShape(4.dp)` on cards/buttons; minimum touch target 48–50dp.
- `statusBarsPadding()` + `navigationBarsPadding()` on root containers.
