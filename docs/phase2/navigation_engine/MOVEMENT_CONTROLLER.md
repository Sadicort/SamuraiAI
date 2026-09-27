# Movement Controller

Clases: `movement.MovementController`, `PathFollower`, `MovementBody`, `MovementMode`, `MovementReport`, `MovementState`, `SmoothRotation`, `DoorInteraction`; cuerpo real `world.MobMovementBody`.

## Reparto

`MovementController` **ejecuta** una ruta un tick a la vez; no la calcula ni decide si tuvo éxito (eso es del runtime). Informa con un `MovementReport`
(`MOVING, ARRIVED, WAITING_DOOR, DOOR_FAILED, OFF_PATH, BODY_LOST`, distancia recorrida, salto, puertas).

## Seguimiento (`PathFollower`)

- *Pure pursuit* sobre tramos rectos: apunta 1,4 bloques por delante en el segmento, de modo que una ruta suavizada se sigue como línea y no como paradas.
- Nodos "precisos" (escalón, salto, caída, puerta, escalera, agua, puente): apunta al centro del bloque.
- Un nodo se da por alcanzado dentro de `reachRadius` **y** con tolerancia vertical según la arista (escalón/salto 0,6, escalera/caída 0,9, resto 1,2). Con tolerancia laxa el controlador daba por
  alcanzado un escalón sin subirlo y saltaba desde abajo (hallazgo de las pruebas).
- Salto en aristas `JUMP` (con enfriamiento de 10 ticks); nunca para `STEP` (escaleras). Escalera de mano: dirección de subida/bajada.
- Desvío del segmento > 3,5 bloques ⇒ `OFF_PATH` (el runtime replanifica).

## Marchas y giro

`WALK/RUN/SPRINT/SNEAK` con velocidades configurables. El factor de velocidad baja con el error de rumbo (`SmoothRotation.turnSpeedFactor`, nunca < 0,15) para que gire sin patinar.
La rotación del cuerpo la hace el `MoveControl` de la entidad (máx. 90°/tick) y la cabeza el `LookControl`; `SmoothRotation` modela el giro progresivo en la lógica y en el simulador.

## Puertas (`DoorInteraction`)

Aproximar → abrir (a ≤ 2,2 bloques) → esperar `doorWaitTicks` → cruzar → cerrar cuando el caminante se aleja ≥ 1,4 (si `closeDoorsBehind`). Al abrir se apunta al **centro del hueco libre** junto a la
hoja (`MovementBody.doorPassPoint`), no al centro del bloque. Hierro: `MobMovementBody.setDoor` lo rechaza, así que el camino no lo usa (`openIronDoors` está en las preferencias, apagado).
Verificado con CustomNPCs real: abre 1, cierra 1, llega.

## Cuerpo (`MobMovementBody`)

`steer` → `MoveControl.setWantedPosition`; `jump` → `JumpControl`; `look` → `LookControl`; `stop` → detiene navegación vanilla y velocidad; `climb` → empuje vertical si `onClimbable()`; puertas por la API del bloque
(`DoorBlock.setOpen`, portillos por propiedad `OPEN`); `width()` desde el hitbox real; `diagnostics()` para trazas.

## Hallazgo importante (CustomNPCs)

Un avatar creado por API conserva las dimensiones iniciales **1×1** hasta `refreshDimensions()`, y no cabe por una puerta. `CustomNPCsController.spawnPhysical` lo llama. Además navegación
toma el control: `setReturnsHome(false)` y `setMovingType(0)` desactivan el deambular propio de CustomNPCs (dos sistemas dirigiendo un cuerpo se pelean).

## Límites

- Teletransporte solo como recuperación opt-in (`allowTeleportRecovery`, apagado).
- Cuerpos de ancho ≥ 1 no atraviesan puertas de 0,81 (se modela con `bodyRadius`; no hay planificación por dimensiones múltiples).
