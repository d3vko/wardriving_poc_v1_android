# RF Village MX Wardriving

Native Android wardriving MVP in Kotlin with Jetpack Compose, Material 3, Room, Retrofit/OkHttp, WorkManager, Kotlin Coroutines/Flow, Hilt, and an Android foreground service.

## Current MVP

- Login, register, and password recovery client support.
- Foreground service `WardrivingForegroundService` with start, pause, resume, and stop commands.
- GPS collection through `FusedLocationProviderClient`.
- Wi-Fi collection through `WifiManager`.
- BLE collection through `BluetoothLeScanner`.
- LTE collection through public Android Telephony APIs, not AT commands.
- Room-backed session/sample history.
- Wi-Fi/BLE Wigle-compatible CSV export, LTE CSV export, ZIP export, sharing, and Storage Access Framework Save As.
- Retryable multipart uploads through WorkManager.
- Optional external AT modem module documented only for future USB OTG implementation.

## Setup

1. Install Android Studio with JDK 17 selected as the Gradle JDK.
2. Install Android SDK 34.
3. Configure API values with Gradle properties or environment variables:

   ```bash
   cp .env.example .env
   ```

   Then edit `.env`:

   ```bash
   API_BASE_URL=https://wardriving-ctf.rf-village-mx.com/wardriving/api/v1/
   API_LOGIN_PATH=auth/login/
   API_REGISTER_PATH=auth/register/
   API_PASSWORD_RECOVERY_PATH=auth/password/reset/
   API_UPLOAD_PATH=files-uploaded/
   API_UPLOAD_TYPE_WIFI_BLE=wifi_ble_android
   API_UPLOAD_TYPE_LTE=lte_android
   APP_ACCENT_COLOR=#00A676
   ```

   The app module reads values in this order: Gradle property, real environment variable, `.env`, default value. Sync Android Studio after editing `.env` because values are compiled into `BuildConfig`.
   Upload logs include the resolved endpoint and payload source, for example `Uploading files=[<file>.csv] device_source=wifi_ble_android to https://wardriving-ctf.rf-village-mx.com/wardriving/api/v1/files-uploaded/`.

4. Open the project in Android Studio and sync Gradle.
5. Run the `app` configuration on a physical device. Wi-Fi/BLE/LTE scanning is not useful on most emulators.

The local shell currently reports Java 8, so command-line Gradle builds need `JAVA_HOME` pointed to JDK 17 first.

## Privacy Disclosure

This app collects nearby Wi-Fi/BLE identifiers, cellular network data, and GPS coordinates for wardriving/research purposes. Upload is disabled by default. Settings include anonymizing SSIDs, anonymizing BLE names, and deleting local data.

## Internal LTE Collection

Internal phone LTE data is collected only with public Android APIs:

- `TelephonyManager.getAllCellInfo()`
- `TelephonyManager.requestCellInfoUpdate()`
- `CellInfoLte`
- `CellIdentityLte`
- `CellSignalStrengthLte`
- `SubscriptionManager` when multi-SIM support is expanded

Raw AT commands are not available for stock Android internal phone modems and are not part of the internal LTE path.

## Tests

Unit tests live under `core/src/test` and cover:

- LTE CSV formatter
- Wi-Fi/BLE Wigle CSV formatter
- Multipart upload part creation
- LTE EARFCN band mapping
- Duplicate filtering

Run after selecting JDK 17:

```bash
./gradlew test
```

Use the included Gradle Wrapper so Android Studio does not pick an incompatible system Gradle:

```bash
./gradlew test
```

## Known Limitations

- Raw AT commands are not available for internal phone modems on stock Android.
- LTE fields vary by device, carrier, Android version, and permissions.
- Wi-Fi scanning may be throttled by Android.
- BLE scanning may return randomized privacy addresses.
- Background scanning requires a visible foreground notification and user-granted permissions.
- Google Play may require background location justification if published publicly.
- The external USB LTE AT modem module is documentation-only in this MVP.
