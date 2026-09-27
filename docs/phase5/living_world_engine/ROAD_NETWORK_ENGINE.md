# Road Network Engine

**Código:** `living/world/roads/RoadNetwork.java`, `RoadNode.java`, `RoadEdge.java`, `RoutePlan.java`.

## Grafo

- **Nodos** (`RoadNode`): puerta de un asentamiento, cruce, puente o hito.
- **Tramos** (`RoadEdge`): tipo (`ROAD, TRAIL, BRIDGE, SHORTCUT`), longitud recorrida (la recta × `roadTortuosity`, 1,25), estado, peligro (media del peligro base de las regiones que cruza + el mayor peligro de eventos entre ellas), puentes (uno por región de río cruzada), bloqueo y su motivo, tráfico y último uso. Cada tramo sabe qué regiones cruza, así que un cambio de peligro solo reevalúa esos.
- Al fundar un asentamiento se une a sus `roadsPerSettlement` (2) vecinos más cercanos dentro de `maxRoadLength` (3000).

## Rutas

`route(desde, hasta, preferencias)` — Dijkstra ponderando la longitud por el peligro según quién viaja (`Preferences`):

| Perfil | Peso del peligro | Senderos | Peligro máximo aceptado |
| --- | --- | --- | --- |
| `CARAVAN` | 4,0 | no | 0,85 |
| `TRAVELLER` | 2,0 | sí | 0,95 |
| `GUARD` | 0,5 | sí | 1,0 |

Los tramos bloqueados se rechazan. El resultado (`RoutePlan`) da nodos, tramos, longitud, peligro máximo y medio, puentes y longitud efectiva. **Caminar** físicamente un tramo sigue siendo trabajo de Navigation; esto es el plan a escala mundo que siguen mercaderes, caravanas, guardias y viajeros.

## Bloqueos

`block(tramo, motivo)` / `reopen(tramo)` publican `RoadBlockedEvent` / `RoadReopenedEvent` y suben la versión del grafo. La economía invalida sus rutas comerciales **por evento** (`roadChanged`) en vez de replanificar por sondeo. Los eventos de mundo que bloquean caminos (`FLOOD`, `WAR`) bloquean los tramos que cruzan su región mientras corren y los reabren al terminar (`eventBlockedRoads`).

## Pruebas

`settlementsAreFoundedWithRoadsAndRoutesAvoidDanger`, `warBlocksRoadsWhileItRunsAndTheyReopenAfterwards`.
