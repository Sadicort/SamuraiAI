# Threading y Scheduler

## Problema

Los callbacks HTTP de Ollama y algunos `CompletableFuture` podían ejecutar
acciones Minecraft desde hilos de red. Eso exponía entidades, memoria, eventos y
controladores a condiciones de carrera.

## Solución

`ServerScheduler` es la única frontera para trabajo del servidor. Se enlaza al
`MinecraftServer` en `ServerStartedEvent`, conserva una sesión y descarta tareas
de sesiones anteriores al detenerse. `AIRequestQueue`, `DialogueService`,
`NPCManager`, `NPCLifecycleManager`, `MemoryManager`, `EmotionService`,
`RelationshipService`, `NPCSpawnService`, `DialogueRouter` y los controladores
validan `requireServerThread()` antes de tocar estado de Minecraft.

Los resultados de Ollama se transforman fuera del hilo principal y se entregan
mediante `ServerScheduler.executor()`. La cancelación también se propaga al
future HTTP.

## Prueba

`CoreTest.backgroundMutationsAreMarshalled` y
`CoreTest.oldSchedulerSessionCannotLeakWork` comprueban mutaciones externas y
aislamiento de sesiones. `gradle check -PwithCustomNpcs=false` arranca el
GameTestServer y confirma que las operaciones ocurren en `Server thread`.

## Riesgos futuros

Toda nueva integración (combate, movimiento o persistencia) debe recibir una
referencia al scheduler, nunca invocar directamente una API de Minecraft desde
un callback asíncrono.
