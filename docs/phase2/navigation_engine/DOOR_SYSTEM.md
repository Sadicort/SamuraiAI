# Door System

Clases: `movement.DoorInteraction`, `MovementController`, `events.DoorOpenedEvent`, `DoorClosedEvent`; `world.MobMovementBody.setDoor/isDoorOpen/doorPassPoint`.

## Pipeline

```
Detectar puerta (arista DOOR) → Acercarse → Abrir → Esperar → Cruzar → Cerrar
```

1. **Detectar**: el grafo marca `EdgeType.DOOR` al entrar en un nodo cuya cavidad es una puerta abrible (madera y portillos). Las de hierro no son transitables salvo `openIronDoors`.
2. **Abrir**: a ≤ 2,2 bloques, `MovementBody.setDoor(pos, true)`. Se normaliza a la mitad inferior. Si la puerta se niega → `DOOR_FAILED` → el nodo se bloquea y se replanifica; si se agotan los recálculos → `DOOR_LOCKED`.
3. **Esperar**: `doorWaitTicks` con el cuerpo parado, mirando la puerta.
4. **Cruzar**: apuntando al centro del hueco libre.
5. **Cerrar**: `closeDoorsBehind` (configurable); solo si **nosotros** la abrimos. Si la sesión termina con una puerta abierta y el caminante ya está lejos, `abandon` la cierra.

## Eventos y métricas

`DoorOpenedEvent(npcId, door)`, `DoorClosedEvent(npcId, door)`; `SessionMetrics.doorsOpened/doorsClosed`.

## Verificación

Simulador: `woodenDoorIsOpenedCrossedAndClosedBehind`, `ironDoorSealsTheWayAndTheRequestFails`. Servidor real con CustomNPCs: `walksThroughAWoodenDoorAndClosesIt`
(abre 1, cierra 1, llega).

## Extensión

Nuevos tipos (puertas de CustomNPCs, trampillas): clasificar en `MinecraftNavWorldView.compute` y manejar en `MobMovementBody.setDoor`.
