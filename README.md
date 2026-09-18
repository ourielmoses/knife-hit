# Dino

Chrome-style endless runner for Android — landscape only.

Package: `com.dino.game` · minSdk 24 · targetSdk 36

## Controls

- **Right half of screen** — Jump (also starts / retries)
- **Left half of screen** — Duck (hold; grounded only; head leans forward)
- **SOUND ON/OFF** (top-left) — mutes procedural SFX

## Features

- Cute pixel T-Rex sprites (run / jump / duck / dead)
- Rising difficulty, cacti + birds after score 200
- High score saved locally; shown on HUD and game over
- **Night mode** every 700 points (palette invert)
- Dust particles, parallax dunes
- Jump / land / die / milestone SFX + hit haptics

## Build & run

```bash
./gradlew installDebug
adb shell am start -n com.dino.game/.MainActivity
```
