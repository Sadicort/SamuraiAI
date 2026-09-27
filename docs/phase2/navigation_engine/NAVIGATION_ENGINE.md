# Navigation Engine v2.2 — Arquitectura general

Paquete raíz: `yadi.samuraiai.ai.navigation`. 94 clases, sin dependencias de Minecraft salvo el paquete `world/`.

## Responsabilidades (y sus fronteras)

| Capa | Clase principal | Hace | Nunca hace |
|---|---|---|---|
| Solicitud | `task.NavigateTask` / `MoveToTask` | pide un viaje y sondea el resultado | calcular rutas |
| Fachada MC | `world.NavigationService` | un motor por dimensión, cuerpo del NPC, tick, eventos de mundo | decidir destinos |
| Supervisión | `engine.NavigationRuntime` | reparte presupuesto, ejecuta movimiento, detecta atascos | mover entidades por sí mismo |
| Núcleo | `engine.NavigationEngine` | validar destino, planificar, verificar, recalcular, recuperar, publicar eventos | mover entidades |
| Algoritmo | `pathfinding.PathfindingEngine` | A\* reanudable, suavizado, validación | conocer NPC |
| Movimiento | `movement.MovementController` | seguir la ruta, saltar, girar, puertas | decidir la ruta |
| Cuerpo | `movement.MovementBody` (`world.MobMovementBody`) | ejecutar en la entidad | decidir nada |

## Flujo

```
Behavior ──MoveToTask──▶ NavigationService.navigate ──▶ NavigationRuntime.request
                                                              │ (cada tick, con presupuesto)
   NavigationEngine.plan ◀── PathSearch.advance(nodos)        ▼
   NavigationEngine.verify (cada N ticks)         MovementController.tick ──▶ MovementBody
   NavigationEngine.onMovement ◀────────── MovementReport
   RecoveryEngine / applyRecovery ──▶ eventos + métricas
```

## Regla de aislamiento

`ai.navigation.{engine,graph,pathfinding,planner,terrain,chunks,obstacles,movement,recovery,cache,zones,metrics,prediction,debug,events}`
no importan `net.minecraft`. Se prueban con un mundo en rejilla (`GridWorldView`) y un cuerpo simulado
(`SimulatedBody`) sin arrancar Minecraft. Solo `ai.navigation.world` toca el juego.

## Integración con lo existente

- `NPCController.movementBody(instance)` (método `default`, compatible): `CustomNPCsController` lo implementa.
- `MoveToTask` delega en navegación cuando hay cuerpo y `navigation.enabled`; si no, conserva su comportamiento anterior.
- `DefaultBrain` cancela la tarea al cambiar de objetivo → `NavigateTask.cancel()` → la sesión se cancela y el cuerpo se detiene
  (antes el NPC seguía caminando tras abandonar la patrulla).
- `NPCSpawnService.remove` → `NavigationService.forget`; `Samuraiai.stopping` → `NavigationService.reset`.
- `PatrolBehavior` usa `NPCInstance.getHome()` (ancla estable): antes derivaba con cada paso.

## Pruebas

45 tests unitarios (`PathfindingTest`, `NavigationRuntimeTest`) y 4 GameTests de navegación (`NavigationGameTests`) con CustomNPCs real.
Ver `PHASE2_TESTING.md`.

## Extensibilidad

Nuevo tipo de arista: `EdgeType` + generación en `NavigationGraph.neighbors` + coste en `PathCostModel`. Nuevo arquetipo:
`NavigationProfiles.register`. Nuevo backend de cuerpo: implementar `MovementBody`. Nuevo peligro: `DangerSource`.
