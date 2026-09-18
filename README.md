# Dino

Chrome-style endless runner for Android — landscape only.

Package: `com.dino.game` · minSdk 24 · targetSdk 36

## Controls

- **Right half of screen** — Jump (also starts / retries)
- **Left half of screen** — Duck (hold; grounded only)

## Build & run

```bash
./gradlew installDebug
adb shell am start -n com.dino.game/.MainActivity
```

## Notes

- High score is saved locally and shown on the HUD and game-over screen.
- Night mode and audio are deferred to a later phase.
