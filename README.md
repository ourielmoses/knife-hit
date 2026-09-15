# Hello World

A minimal native Android "Hello, World!" app built with **Kotlin + Jetpack Compose**.

- Package: `com.example.helloworld`
- minSdk 24 · targetSdk 36 · compileSdk 36

## Requirements

- JDK 17 or 21
- Android SDK platform 36 and build-tools 36
- Set `ANDROID_HOME`, or create `local.properties` with:

```
sdk.dir=/path/to/android-sdk
```

(Android Studio creates `local.properties` automatically.)

## Build

Debug APK:

```bash
./gradlew assembleDebug
```

APK path:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Run on a connected phone

1. Enable **Developer options** (tap Build number 7×) and **USB debugging** on the phone.
2. Plug the phone into your computer and accept the **Allow USB debugging** prompt.
3. Confirm it is visible: `adb devices` (should list the device as `device`).
4. Build, install, and launch:

```bash
./gradlew installDebug
adb shell am start -n com.example.helloworld/.MainActivity
```

Or open the folder in Android Studio, pick your device, and press **Run**.
