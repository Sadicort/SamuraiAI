# Lifecycle, Runtime y spawn

## Problema

El registro, el runtime y el controlador podían sobrevivir a la eliminación del
NPC, generando NPCs fantasma y tareas pendientes.

## Solución

`NPCLifecycleManager` valida las transiciones `CREATING → INITIALIZING → ACTIVE
→ UNLOADING → INACTIVE/REMOVED` y permite reactivación desde `INACTIVE`. El
`NPCSpawnService` ejecuta un spawn transaccional con rollback, activa runtime y
eventos sólo tras alcanzar `ACTIVE`, y en remove cancela diálogo, Brain, tareas,
controller, índices, memoria y relaciones antes de olvidar el lifecycle.

`NPCTickService` elimina automáticamente un NPC activo cuyo cuerpo físico ya no
existe. `NPCRuntime` no puede marcarse activo fuera del hilo principal y el Brain
sólo piensa cuando lifecycle y runtime están activos.

## Prueba

`CoreTest.lifecycleRejectsInvalidTransitions` y el GameTest `phase1SpawnAndRemove`
comprueban spawn, status, índices, remove e inexistencia física posterior.

## Riesgos futuros

La reactivación sólo deja hooks; todavía no restaura estado persistente. La fase 2
debe conectar behaviors a `cancelAll()` y a las transiciones de unload.
