# Fase 4 — Persistencia y memoria de largo plazo

`SamuraiWorldData` implementa `SavedData` por mundo y guarda snapshots NBT con
UUID, esquema, tipo, nombre, personalidad y ubicación. `PersistenceBootstrap`
expone `saveWorld`, `loadWorld` y `forget`; el servidor guarda antes de retirar
NPCs durante `ServerStoppingEvent` y elimina snapshots en un remove normal.

Al iniciar, los snapshots se leen y se reactivan mediante
`NPCSpawnService.restore`, conservando la identidad UUID y la descripción de
personalidad serializada. La restauración es
server-thread-only y una entrada corrupta se aísla sin impedir el arranque.

La memoria conversacional sigue siendo runtime; `MemoryKind` y
`MemoryAccess` dejan los espacios para working, episódica, semántica y largo
plazo sin inventar un formato prematuro. La siguiente iteración debe persistir
resúmenes y relaciones con migraciones de esquema y escritura atómica.

## Pruebas

`CoreTest.snapshotRoundTripAndUnknownVersion` cubre el DTO; la verificación de
reinicio requiere dos sesiones Forge sobre el mismo mundo.
