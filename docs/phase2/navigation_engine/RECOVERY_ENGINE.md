# Recovery Engine

Clases: `recovery.StuckDetector`, `RecoveryEngine`, `RecoveryAction`; orquestación en `engine.NavigationEngine` y `NavigationRuntime`.

## Detección

`StuckDetector`: ancla la posición; si en `stuckTicks` (40) el caminante que intenta moverse no se ha alejado `stuckMinProgress` (0,35) del ancla, está atascado.

## Escalera (determinista)

| Intento | Acción | Efecto |
|---|---|---|
| 0 | `JUMP_NUDGE` | salta en el sitio |
| 1 | `RECALCULATE` | misma meta, búsqueda nueva desde donde está |
| 2 | `ALTERNATIVE_ROUTE` | replanifica evitando las 2 celdas inmediatas de la ruta (nunca la meta) |
| 3 | `BACKTRACK` | retrocede 2 nodos |
| 4 | `WAIT` (o `SAFE_TELEPORT` si `allowTeleportRecovery`) | pausa 20 ticks / reubica a ~4 bloques por delante en la ruta, validado |
| ≥ máx. | `CANCEL` | falla con `STUCK`; el comportamiento elige otra cosa |

`maxRecoveryAttempts` (4) y `maxRecalculations` (6) son configurables; cada paso publica `NPCStuckEvent` y cuenta métricas.

## Otros fallos recuperables

Obstáculo estático → desvío local; entidad → espera y desvío; fuera de ruta → replanifica; chunk descargado → espera acotada; puerta bloqueada → nodo bloqueado y replanifica;
demasiados recálculos → `TOO_MANY_RECALCULATIONS`; plazo → `TIMEOUT`.

## Errores encontrados por las pruebas

Bloquear los nodos de la ruta como "alternativa" bloqueaba la **meta** (los tramos suavizados tienen pocos nodos): ahora se bloquean celdas por delante y la meta nunca. El teletransporte
elegía "3 nodos adelante", que en una ruta suavizada era la meta: ahora es un punto validado a ~4 bloques.

## Verificación

`frozenWalkerEscalatesThroughRecoveryAndFailsAsStuck`, `teleportRecoveryIsOptInAndRelocatesToAValidatedNode`, `deadlineEndsTheSessionWithTimeout`, `walkerPushedOffPathReplansFromItsRealPosition`.
