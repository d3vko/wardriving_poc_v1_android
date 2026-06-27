# RF Village MX Wardriving

Native Android wardriving MVP in Kotlin with Jetpack Compose, Material 3, Room, Retrofit/OkHttp, Kotlin Coroutines/Flow, Hilt, and an Android foreground service.

## Current MVP

- Login, register, and password recovery client support.
- Foreground service `WardrivingForegroundService` with start, pause, resume, and stop commands.
- GPS collection through `FusedLocationProviderClient`.
- Wi-Fi collection through `WifiManager`.
- BLE collection through `BluetoothLeScanner`.
- LTE collection through public Android Telephony APIs, not AT commands.
- Room-backed session/sample history.
- Wi-Fi/BLE Wigle-compatible CSV export, LTE CSV export, ZIP export, sharing, and Storage Access Framework Save As.
- Multipart uploads from the session detail and export screens; pending upload records are stored locally in Room.
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
   # Map basemap — uncomment the MAPLIBRE_STYLE_URL you wish to use.
   # Gallery (free, no API key): https://madewithmaplibre.com/basemaps/gallery
   #
   # OpenFreeMap
   # MAPLIBRE_STYLE_URL=https://tiles.openfreemap.org/styles/liberty
   # MAPLIBRE_STYLE_URL=https://tiles.openfreemap.org/styles/bright
   # MAPLIBRE_STYLE_URL=https://tiles.openfreemap.org/styles/positron
   # MAPLIBRE_STYLE_URL=https://tiles.openfreemap.org/styles/dark
   # MAPLIBRE_STYLE_URL=https://tiles.openfreemap.org/styles/fiord
   #
   # OSM Americana
   # MAPLIBRE_STYLE_URL=https://americanamap.org/style.json
   #
   # VersaTiles
   # MAPLIBRE_STYLE_URL=https://tiles.versatiles.org/assets/styles/colorful/style.json
   #
   # MapLibre Demotiles (default)
   MAPLIBRE_STYLE_URL=https://demotiles.maplibre.org/style.json
   #
   # Not configurable via MAPLIBRE_STYLE_URL (inline style JSON required):
   # - Google Satellite: https://madewithmaplibre.com/basemaps/styles/google-satellite/
   # - Terrarium Elevation: https://madewithmaplibre.com/basemaps/styles/aws-terrarium/
   ```

   The app module reads values in this order: Gradle property, real environment variable, `.env`, default value. Sync Android Studio after editing `.env` because values are compiled into `BuildConfig`.
   Upload logs include the resolved endpoint and payload source, for example `Uploading files=[<file>.csv] device_source=wifi_ble_android to https://wardriving-ctf.rf-village-mx.com/wardriving/api/v1/files-uploaded/`.

4. Open the project in Android Studio and sync Gradle.
5. Run the `app` configuration on a physical device. Wi-Fi/BLE/LTE scanning is not useful on most emulators.

The local shell currently reports Java 8, so command-line Gradle builds need `JAVA_HOME` pointed to JDK 17 first.

## Map basemaps

The map screen uses MapLibre with a style URL compiled from `MAPLIBRE_STYLE_URL` in `.env`. Browse free basemaps (no API key) in the [MapLibre Basemap Gallery](https://madewithmaplibre.com/basemaps/gallery).

To switch styles, uncomment one `MAPLIBRE_STYLE_URL` line in `.env` (and comment out the active one), then sync Gradle in Android Studio so `BuildConfig` picks up the new value.

| Style | Provider | Description | Style URL |
| --- | --- | --- | --- |
| Liberty | OpenFreeMap | Free and open street map with a classic look, based on OSM Liberty. | `https://tiles.openfreemap.org/styles/liberty` |
| Bright | OpenFreeMap | Bright, colorful map style based on OSM Bright. | `https://tiles.openfreemap.org/styles/bright` |
| Positron | OpenFreeMap | Light, minimal basemap with subtle gray tones. Great as a background for data overlays. | `https://tiles.openfreemap.org/styles/positron` |
| Dark | OpenFreeMap | Dark basemap with muted colors, ideal for data visualization and night-mode UIs. | `https://tiles.openfreemap.org/styles/dark` |
| Fiord | OpenFreeMap | Blue-toned dark style with a Nordic feel. | `https://tiles.openfreemap.org/styles/fiord` |
| OSM Americana | OSM Americana | Road-atlas-inspired style with highway shields, route markers, and American cartographic conventions. | `https://americanamap.org/style.json` |
| Colorful | VersaTiles | Vibrant, colorful basemap with rich detail. Fully free with no API key or usage limits. | `https://tiles.versatiles.org/assets/styles/colorful/style.json` |
| Demotiles | MapLibre | Built-in demo style showing country boundaries and labels. Useful for testing. (default) | `https://demotiles.maplibre.org/style.json` |
| Google Satellite | Google | High-resolution satellite imagery raster tiles. | Not configurable via `MAPLIBRE_STYLE_URL` — see [gallery entry](https://madewithmaplibre.com/basemaps/styles/google-satellite/) |
| Terrarium Elevation | AWS Open Data | Global elevation tiles in Terrarium format. Use for hillshading and 3D terrain. | Not configurable via `MAPLIBRE_STYLE_URL` — see [gallery entry](https://madewithmaplibre.com/basemaps/styles/aws-terrarium/) |

**Google Satellite** and **Terrarium Elevation** are listed as free in the gallery, but they require an inline MapLibre style JSON (raster or `raster-dem` sources) rather than a hosted `style.json` URL. The app currently loads the map with `map.setStyle(BuildConfig.MAPLIBRE_STYLE_URL)`, so only the eight URL-based styles above can be selected through `.env` without code changes.

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
