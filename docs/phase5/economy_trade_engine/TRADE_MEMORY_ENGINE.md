# Trade Memory Engine

**Código:** `living/economy/memory/TradeMemory.java`.

- **Instantánea diaria** de cada asentamiento: precios, existencias, producción, consumo, importaciones, exportaciones y déficits, conservada `memoryDays` (60) días.
- **Registro de acontecimientos:** escaseces, caravanas perdidas, rutas bloqueadas, cosechas grandes y malas, acuerdos.
- La leen las misiones, la depuración y (a través de la comunidad y las conversaciones) el conocimiento de los NPCs.
- Libro de movimientos (`ledger/EconomyLedger`): cada movimiento `ORIGEN → MOVIMIENTO → DESTINO` con valor, referencia y procedencia; se guardan los últimos `ledgerMax` (4000) completos en el mundo y `ledgerPerSettlement` (400) por asentamiento, y los **totales** por tipo y recurso para siempre.
