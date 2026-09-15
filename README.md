# Knife Hit

Native Android arcade game in **Kotlin + Jetpack Compose**. Throw knives into a spinning target, clear stages, beat world bosses, and unlock skins.

Package: `com.knifehit.game` · minSdk 24 · targetSdk 36 · portrait locked.

## Requirements

- JDK 17 or 21
- Android SDK platform 36 and build-tools 36
- Set `ANDROID_HOME` (or `sdk.dir` in `local.properties`)

```
sdk.dir=/path/to/android-sdk
```

## Build

Player debug APK (the one you sideload to play):

```bash
./gradlew assemblePlayerDebug
```

APK path:

```
app/build/outputs/apk/player/debug/app-player-debug.apk
```

Admin debug APK (developer panel, separate application id so it installs beside the player build):

```bash
./gradlew assembleAdminDebug
```

Release variants (unsigned unless you add a signing config):

```bash
./gradlew assemblePlayerRelease assembleAdminRelease
```

Unit tests (collision, angles, capacity, level generation):

```bash
./gradlew testPlayerDebugUnitTest
```

## Run / sideload

```bash
adb install -r app/build/outputs/apk/player/debug/app-player-debug.apk
adb shell am start -n com.knifehit.game/com.knifehit.game.MainActivity
```

Admin:

```bash
adb install -r app/build/outputs/apk/admin/debug/app-admin-debug.apk
adb shell am start -n com.knifehit.game.admin/com.knifehit.game.MainActivity
```

From Android Studio: open this folder, pick the `playerDebug` or `adminDebug` run configuration, and run on a device or emulator.

## How to play

Tap anywhere to throw. Stick every knife to clear the stage. Hitting a planted hilt is game over. The last stage of each world is the boss. Failing without a revive returns you to **stage 1 of the current world** (unlocks, wallet, and high score stay).

## Admin flavor

The player APK does **not** compile admin UI. The admin app shows a **DEVELOPER PANEL** on the home screen, behind a passcode.

Default passcode: `knifehit`  
Override at build time (stored as a SHA-256 hash in BuildConfig, not plaintext):

```bash
./gradlew assembleAdminDebug -PadminPasscode=your-secret
```

Unlock lasts for the process; the panel re-locks after the app has been backgrounded for two minutes. Export/import JSON moves content overrides between the admin sandbox and a player install.

## Project layout

```
com.knifehit.game/
  MainActivity.kt          immersive activity
  KnifeHitApp.kt           screens, economy, ads, persistence glue
  model/                   skins, worlds, attachments, constants
  engine/                  loop, swept collision, levels, particles
  render/                  canvas draw (state read in draw phase)
  audio/                   procedural AudioTrack SFX + ambient
  data/                    DataStore save + content overrides
  ads/                     AdProvider seam + mock
  billing/                 BillingProvider seam (no-op, no IAP UI)
  ui/                      Home, Shop, Locker, Worlds, Settings, ads
  i18n/                    en / he string provider
  admin/                   flavor source sets (real panel vs empty stub)
```

## Notes

- Payments: `BillingProvider` exists; nothing in the shop charges real money.
- Ads: in-game mock overlay only (`MockAdProvider`). Rewards grant on `Completed` only.
- Localization: overlays follow RTL in Hebrew; the HUD canvas (ammo, score digits) does not mirror.
- High scores are skipped for runs that used a revive (`isCurrentRunCheated`). Currency and world unlocks still apply.
