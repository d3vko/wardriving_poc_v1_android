# CSV Formats

## Wi-Fi/BLE CSV

File name:

```text
wifi_ble_<session_id>_<timestamp>.csv
```

Header:

```text
MAC,SSID,AuthMode,FirstSeen,Channel,RSSI,CurrentLatitude,CurrentLongitude,AltitudeMeters,AccuracyMeters,Type
```

Wi-Fi mapping:

- `BSSID` -> `MAC`
- `SSID` -> `SSID`
- `capabilities` -> `AuthMode`
- `frequency` -> channel
- `level` -> `RSSI`
- latest GPS fix -> coordinates
- `Type` -> `WIFI`

BLE mapping:

- Bluetooth address when available -> `MAC`
- device name or empty string -> `SSID`
- `BLE` -> `AuthMode`
- empty channel unless a future mapper derives one
- `ScanResult.rssi` -> `RSSI`
- latest GPS fix -> coordinates
- `Type` -> `BLE`

## LTE CSV

File name:

```text
lte_<session_id>_<timestamp>.csv
```

Header:

```text
Timestamp,Technology,State,MCC,MNC,LAC,CellID,Band,RSSI,RSRP,RSRQ,SINR,Operator,Longitude,Latitude
```

Android API mapping:

- `CellIdentityLte.mccString` -> `MCC`
- `CellIdentityLte.mncString` -> `MNC`
- `CellIdentityLte.tac` -> `LAC`
- `CellIdentityLte.ci` -> `CellID`
- `CellIdentityLte.earfcn` -> derived `Band` when supported
- `CellSignalStrengthLte.dbm` -> `RSSI`
- `CellSignalStrengthLte.rsrp` -> `RSRP`
- `CellSignalStrengthLte.rsrq` -> `RSRQ`
- `CellSignalStrengthLte.rssnr` -> `SINR`
- `TelephonyManager.networkOperatorName` -> `Operator`
- latest GPS fix -> `Longitude`, `Latitude`

Unavailable Android values such as `CellInfo.UNAVAILABLE` are exported as empty strings.
