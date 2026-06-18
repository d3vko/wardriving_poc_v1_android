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
Timestamp,Tecnología,TipoCelda,Estado,MCC,MNC,LAC,CellID,eNodeB,Sector,PCI,Banda,EARFCN,FreqDL_MHz,FreqUL_MHz,RSSI,RSRP,RSRQ,SINR,Operador,Longitud,Latitud
```

Android API mapping:

- `CellInfoLte` -> `Tecnología` and `TipoCelda` as `LTE`
- `CellInfo.cellConnectionStatus` -> `Estado`
- `CellIdentityLte.mccString` -> `MCC`
- `CellIdentityLte.mncString` -> `MNC`
- `CellIdentityLte.tac` -> `LAC`
- `CellIdentityLte.ci` -> `CellID`
- `CellIdentityLte.ci / 256` -> `eNodeB`
- `CellIdentityLte.ci % 256` -> `Sector`
- `CellIdentityLte.pci` -> `PCI`
- `CellIdentityLte.earfcn` -> `EARFCN`, derived `Banda`, `FreqDL_MHz`, and `FreqUL_MHz` when mapped
- `CellSignalStrengthLte.dbm` -> `RSSI`
- `CellSignalStrengthLte.rsrp` -> `RSRP`
- `CellSignalStrengthLte.rsrq` -> `RSRQ`
- `CellSignalStrengthLte.rssnr` -> `SINR`
- `TelephonyManager.networkOperatorName` -> `Operador`
- latest GPS fix -> `Longitud`, `Latitud`

Unavailable Android values such as `CellInfo.UNAVAILABLE` are exported as empty strings.
