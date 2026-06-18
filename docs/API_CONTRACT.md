# API Contract

All routes are injected through BuildConfig values or environment/Gradle properties.

## Authentication

### Login

`POST {API_LOGIN_PATH}`

Request:

```json
{
  "username": "operator",
  "email": null,
  "password": "secret"
}
```

If the identifier contains `@`, the app sends it as `email`; otherwise it sends it as `username`.

Response:

```json
{
  "refresh": "refresh-token",
  "access": "access-jwt",
  "username": "operator"
}
```

The app persists `access` with Jetpack Security encrypted shared preferences and sends it as:

```http
Authorization: Bearer <access>
```

`refresh` is accepted in the response but is not used until a refresh endpoint is defined.

### Register

`POST {API_REGISTER_PATH}`

Uses the same request/response shape as login.

### Password Recovery

`POST {API_PASSWORD_RECOVERY_PATH}`

Request:

```json
{
  "username": "operator",
  "email": null
}
```

## Upload

`POST {API_BASE_URL}{API_UPLOAD_PATH}`

Current environment shape:

```bash
API_BASE_URL=https://www.wardriving.lat/wardriving/api/v1/
API_UPLOAD_PATH=files-uploaded/
```

Resolved upload endpoint:

```http
POST https://www.wardriving.lat/wardriving/api/v1/files-uploaded/
```

Multipart form-data parts:

- `files`: one or more CSV files
- `device_source`: `wifi_ble_android` for Wi-Fi/BLE exports or `lte_android` for LTE exports

Equivalent payload shape:

```json
{
  "files": ["@archivo_0", "@archivo_1"],
  "device_source": "wifi_ble_android"
}
```

LTE uploads use the same shape with:

```json
{
  "files": ["@archivo_0"],
  "device_source": "lte_android"
}
```

Uploads are queued as pending local records and retried with WorkManager. Temporary CSV files are not deleted until upload succeeds or the user confirms cleanup in a future cleanup flow.
