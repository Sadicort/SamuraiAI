# Eventos de navegación

Paquete `ai.navigation.events`. Todos implementan `NpcEvent` y viajan por `NPCEventBus` (EventBus 2.0), a través de la interfaz `EventSink` (en pruebas, una lista).

| Evento | Campos | Cuándo |
|---|---|---|
| `PathRequestedEvent` | npc, path, from, to, behavior | se abre la sesión |
| `PathCreatedEvent` | npc, path, nodes, cost, cached, partial | hay ruta (búsqueda o caché) |
| `PathStartedEvent` | npc, path | `READY → RUNNING` |
| `PathBlockedEvent` | npc, path, reason, at | obstáculo, puerta cerrada, chunk descargado… |
| `PathRecalculatedEvent` | npc, path, attempt, reason | una búsqueda de recálculo termina |
| `PathCompletedEvent` | npc, path, ticks, distance | llegada |
| `PathCancelledEvent` | npc, path, reason | cancelación (cambio de objetivo, comando, reemplazo, NPC eliminado) |
| `PathFailedEvent` | npc, path, reason (`NavigationFailure`), detail | fallo explícito |
| `NPCStuckEvent` | npc, path, at, stuckTicks | cada detección de atasco |
| `DoorOpenedEvent` / `DoorClosedEvent` | npc, door | puertas |

## Contrato

Son **hechos**, nunca órdenes. El Brain/Scheduler pueden reaccionar (p. ej. `PathFailedEvent(STUCK)` → elegir otro comportamiento) sin llamar a navegación.
`NavigationFailure`: `DESTINATION_INVALID, DESTINATION_UNREACHABLE, START_INVALID, CHUNK_UNLOADED, SEARCH_LIMIT, NO_BODY, BODY_LOST, TIMEOUT, STUCK, BLOCKED, DANGER, DIMENSION_CHANGED, TOO_MANY_RECALCULATIONS, DOOR_LOCKED, INTERNAL_ERROR`.

## Consumo

`NPCEventBus.getInstance().subscribe(PathFailedEvent.class, handler)`; el `GameTest` de puertas cuenta `DoorOpenedEvent/DoorClosedEvent`.
