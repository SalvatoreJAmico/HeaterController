# HeaterController

HeaterController is an Android app (Jetpack Compose + Kotlin) that reads temperatures/humidity from Govee sensors and toggles Govee devices (heaters/lamp) via the Govee Developer API.

## Setup (Secrets Not Committed)

This repository intentionally does not include any API keys or keystore files. Provide configuration locally using one of these options:

- local.properties (recommended, not tracked):

```
# Govee API
API_KEY=your_govee_api_key
API_KEY_DEBUG=your_debug_key_optional
API_KEY_RELEASE=your_release_key_optional

# Sensor devices (example IDs/models)
GOVEE_INSIDE_DEVICE=...
GOVEE_INSIDE_MODEL=H5100
GOVEE_TANKS_DEVICE=...
GOVEE_TANKS_MODEL=H5100
GOVEE_WATER_DEVICE=...
GOVEE_WATER_MODEL=H5100

# Outputs (heaters/lamp)
GOVEE_HEATER_A_DEVICE=...
GOVEE_HEATER_A_MODEL=H5080
GOVEE_HEATER_B_DEVICE=...
GOVEE_HEATER_B_MODEL=H5080
GOVEE_LAMP_DEVICE=...
GOVEE_LAMP_MODEL=H5080

# Weather (optional)
WEATHER_API_KEY=...
WEATHER_LOCATION=City,ST,Country
```

- Environment variables (CI or local): `API_KEY`, `API_KEY_DEBUG`, `API_KEY_RELEASE`, etc.

The app reads these values in `app/build.gradle.kts` and injects them into `BuildConfig` for Runtime use.

## Building

```
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

Release builds require signing.

## Signing (Local Only)

Generate a keystore and `signing.properties` locally (both are ignored by Git):

```
keytool -genkeypair -v -keystore app/keystore/heatercontroller.jks -alias heatercontroller \
  -keyalg RSA -keysize 2048 -validity 36500

# signing.properties (do not commit)
storeFile=app/keystore/heatercontroller.jks
storePassword=...         # same as keyPassword for PKCS12
keyAlias=heatercontroller
keyPassword=...
```

The Gradle `release` signing config reads from `signing.properties`.

## Install on Device

```
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell am start -n com.salvatore.heatercontroller/.MainActivity
```

## Notes

- Secrets are intentionally excluded via `.gitignore` (`local.properties`, `signing.properties`, `*.jks`).
- Build outputs and generated sources are excluded.
- If you publish this repo publicly, ensure your fork does not include any credentials in commit history.
