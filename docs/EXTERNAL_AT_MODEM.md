# Optional ExternalAtModemModule

This MVP does not implement USB serial runtime behavior. The internal phone modem path must never depend on AT commands.

Future external USB OTG modem support should include:

- USB serial permission flow.
- Vendor profiles: SIMCom SIM7600, Quectel generic, Custom.
- Region profiles: Mexico, North America, Latin America, Custom bands.
- Explicit advanced-mode gate for band locking or region-specific commands.
- No destructive commands by default.

## SIMCom SIM7600-like Read Commands

- `ATI`
- `AT+CGMI`
- `AT+CGMM`
- `AT+CGMR`
- `AT+CSQ`
- `AT+COPS?`
- `AT+CEREG?`
- `AT+CPSI?`
- `AT+CNMP?`
- `AT+CNBP?`
- `AT+CGREG?`
- `AT+CEREG=2`
- `AT+CPSI?`

## Quectel-like Read Commands

- `ATI`
- `AT+CGMI`
- `AT+CGMM`
- `AT+CSQ`
- `AT+COPS?`
- `AT+CEREG?`
- `AT+QENG="servingcell"`
- `AT+QNWINFO`
- `AT+QCAINFO`

If an external modem returns signal values in tenths of dBm, normalize before export or store both raw and normalized values with clearly documented units.
