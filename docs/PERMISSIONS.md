# Permissions

The app requests permissions based on Android version and enabled scanner features.

Location is required because Wi-Fi and BLE scan results are location-derived, GPS geopositions wardriving samples, and LTE cell samples need GPS coordinates.

Declared permissions include:

- `ACCESS_FINE_LOCATION`
- `ACCESS_COARSE_LOCATION`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_LOCATION`
- `FOREGROUND_SERVICE_CONNECTED_DEVICE`
- `FOREGROUND_SERVICE_DATA_SYNC`
- `BLUETOOTH_SCAN`
- `BLUETOOTH_CONNECT`
- `ACCESS_WIFI_STATE`
- `CHANGE_WIFI_STATE`
- `NEARBY_WIFI_DEVICES`
- `READ_PHONE_STATE`
- `ACCESS_NETWORK_STATE`
- `INTERNET`
- `POST_NOTIFICATIONS`

`ACCESS_BACKGROUND_LOCATION` is intentionally not requested in the MVP. Collection starts from visible UI and continues through a foreground service with a persistent notification.
