# Fase 2.2 — Navigation Engine: registro de cambios

Fecha: 2026-09-25.

## Estado del gate

`FOUNDATION_STATUS.json` sigue en **PARTIAL** (`phase2Unlocked: false`). Esta implementación se realizó por instrucción explícita del propietario del proyecto; **no** se modificó ese estado ni se marcó READY.
El documento maestro menciona una "Fase 2.1 (Behavior Engine Core)" que no existe como tal en el repositorio: se construyó sobre el Brain/Behavior/Task/Action existentes y se ampliaron `MoveToTask`,
`NPCController`, `PatrolBehavior` y `NPCInstance` de forma compatible.

## Añadido

- 94 clases en `yadi.samuraiai.ai.navigation.*` (≈4.400 líneas) — ver `NAVIGATION_ENGINE.md`.
- `NavigateTask`; `MoveToTask` delega en navegación (con fallback al comportamiento anterior).
- `NPCController.movementBody` y `ownedEntityIds` (métodos `default`); `CustomNPCsController` los implementa; `MobMovementBody` sobre `Mob`.
- `NPCInstance.getHome()` y `PatrolBehavior` con ancla estable.
- Configuración `samuraiai-navigation.toml` (`NavigationConfig`, 38 valores) + `NavigationSettings.Builder`.
- Comandos `/samuraiai nav …`; overlay de partículas; loggers `SamuraiAI/Navigation` y `SamuraiAI/Performance`.
- 45 tests unitarios y 4 GameTests de navegación (puerta, escalón, replanificación, comandos).

## Corregido (hallado por las pruebas)

| Hallazgo | Dónde apareció | Corrección |
|---|---|---|
| `MoveToTask.cancel()` no detenía al NPC | análisis previo | `NavigateTask.cancel` cancela la sesión y detiene el cuerpo |
| Patrulla que deriva | análisis previo | ancla `getHome()` |
| Ruta alternativa bloqueaba la meta | simulador | se bloquean celdas por delante; la meta nunca |
| Teletransporte de recuperación iba a la meta | simulador | punto validado ~4 bloques por delante |
| Suavizado/validación ignoraban obstáculos y peligro a mitad de tramo | simulador | muestreo de la polilínea y de la huella |
| Escalones dados por alcanzados sin subir; salto desde abajo | simulador | tolerancia vertical por tipo de arista |
| `DoorClosedEvent` nunca se publicaba | simulador | `DoorInteraction.lastClosed` |
| El NPC se detectaba a sí mismo como obstáculo | **servidor real** | excluir `MovementBody.entityId()` |
| Suavizado cortaba esquinas de muros | **servidor real** | comprobación de la huella (radio del cuerpo) |
| Avatar de CustomNPCs con hitbox 1×1 | **servidor real** | `refreshDimensions()` + `MovementBody.width()` en la planificación |
| CustomNPCs rechaza `setWanderingRange(0)` | **servidor real** | se elimina la llamada (basta `MovingType=0`) |

## Verificación (2026-09-25)

`gradlew check` y `gradlew check -PwithCustomNpcs=true`: 130 pruebas unitarias (1 omitida, 0 fallos), 5/5 GameTests en cada perfil (4 de navegación + el de ciclo de vida), `verifyDistributionJar` correcto.

## Pendiente / límites conocidos

Ver `PHASE2_FINAL_REPORT.md` (a completar al cierre de las Fases 2.3 y 2.4): escaleras de mano y de piedra solo en simulador; natación vertical; salto de huecos; overlay sin verificación visual;
sin prueba de carga con cientos de NPC reales (solo simulada); cuerpos de ancho ≥ 1 en puertas.
