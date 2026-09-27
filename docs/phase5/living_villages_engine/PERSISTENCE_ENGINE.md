# Persistencia de aldeas

**Código:** `living/village/persistence/VillageStorage.java` sobre `living/core/persistence/LivingStorage`.

- `village/index.json` (dominio `village-index`): la lista de aldeas.
- `village/villages/<uuid>.json` (dominio `village`), **uno por aldea**: edificios, distritos, ciudadanos de esa aldea, casas, turno de noche, visitantes, eventos, seguridad, trazado, renombre y contadores. Un cambio en una aldea reescribe solo esa aldea.
- Las aldeas nuevas registran su fichero al vuelo (también si se crean mientras se carga).
- Sobre `VersionedStore`: escritura segura (temporal + mover, se conserva el anterior como copia), checksum, recuperación desde la copia, migraciones por versión. Una sección escrita por una versión más nueva se **bloquea**: funciona con valores por defecto esa sesión y no se sobrescribe.

Prueba: `villagesSurviveARestart`, `LivingWorldTest.endToEndTheWorldLivesPersistsAndComesBack`, GameTest de persistencia. Visión global: `../PHASE5_PERSISTENCE.md`.
