# Visitor Engine

**Código:** `living/village/visitors/VisitorEngine.java`, `VisitorRecord.java`.

- Tipos: mercader, viajero, monje, mensajero, samurái, peregrino; con entidad o solo simulados (`npc` nulo).
- Fases: `ARRIVE → STAY → INTERACT → LEAVE → GONE`; `VisitorArrivedEvent` / `VisitorLeftEvent`.
- **Llegadas diarias** (en `simulate`, también en simulación abstracta, como mucho los últimos 30 días al ponerse al día): esperanza `visitorsPerDay` (0,8) × (0,5 + tráfico) × seguridad (1 − amenaza/100, mínimo 0,1) × 1,8 en días sagrados; peregrinos a los templos en días sagrados; samuráis y monjes de vez en cuando. Estancia entre `visitorMinStayHours` (6) y `visitorMaxStayHours` (36); como mucho `maxVisitors` (30).
- **Mercaderes de caravana:** el hub añade un visitante mercader cuando llega una caravana (`CaravanArrivedEvent`).
- Los visitantes comen y compran mientras están (la economía los cuenta como bocas y clientes).

Límite: el «tráfico» usado hoy es fijo (1 camino); no se lee aún el número real de caminos de la red del mundo.

Prueba: `visitorsComeAndGoWhileTheVillageIsSimulated`.
