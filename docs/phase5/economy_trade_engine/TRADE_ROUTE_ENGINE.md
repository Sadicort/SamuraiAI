# Trade Route Engine

**Código:** `living/economy/trade_routes/TradeRoute.java`; `EconomyEngine.route/roadChanged`.

- Una ruta entre dos asentamientos sobre los caminos del mundo: tramos (tramo, longitud, peligro al planificar), distancia, seguridad, peligro, puentes, estado (`OPEN, DANGEROUS, BLOCKED`), versión de la red contra la que se planificó y uso (enviadas, llegadas, perdidas, valor transportado).
- Se **replanifica solo si la red cambió** (versión distinta); se pide la ruta al World Engine con el perfil `CARAVAN` a través de `EconomyPorts.World`.
- **Invalidación por evento:** `RoadBlockedEvent`/`RoadReopenedEvent` → `roadChanged` marca las rutas que usan ese tramo → `TradeRouteDisruptedEvent` (misión «ruta bloqueada» con la posición del tramo y el evento que la causa) / `TradeRouteRestoredEvent` (resuelve la misión).

`/samuraiai living economy routes` (op).
