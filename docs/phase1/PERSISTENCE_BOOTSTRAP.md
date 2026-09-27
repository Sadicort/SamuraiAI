# Infraestructura de persistencia

La persistencia completa queda fuera de Fase 1. `NPCSnapshot` define un DTO con
`schemaVersion`, identidad, tipo, personalidad y ubicación. `PersistenceBootstrap`
ofrece serialización Gson, deserialización validada por versión y hooks de
activación/desactivación sin escribir automáticamente relaciones ni memoria
duradera.

Los hooks se invocan desde `NPCSpawnService`, manteniendo separado Runtime de
persistencia. Versiones desconocidas se rechazan con un error claro. La prueba
`CoreTest.snapshotRoundTripAndUnknownVersion` cubre ambos caminos.

Riesgo futuro: elegir almacenamiento atómico y migraciones antes de activar
guardado real; nunca bloquear el hilo del servidor con I/O.
