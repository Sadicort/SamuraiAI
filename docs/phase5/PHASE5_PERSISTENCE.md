# Fase 5 — Persistencia

**Código:** `living/core/persistence/LivingStorage.java`, `StoreSection.java`, secciones `*/persistence/*Storage.java`, sobre `ai/cognition/storage/VersionedStore` (formato `samuraiai-living`).

## Dónde

`<mundo>/data/samuraiai/living/`:

| Fichero | Contenido |
| --- | --- |
| `calendar/clock.json` | minuto oficial, fracción, retrocesos, saltos, último día procesado, semilla, hora real del guardado |
| `calendar/weather.json` | celdas de clima con su historia |
| `calendar/timeline.json` | cronología mundial |
| `calendar/anniversaries.json` | aniversarios |
| `calendar/agriculture.json` | temporadas de crecimiento y rendimientos fijados |
| `world/regions.json` | regiones (depósitos, fauna, memoria) |
| `world/settlements.json` | asentamientos |
| `world/roads.json` | grafo de caminos |
| `world/events.json` | eventos abiertos y archivo |
| `world/population.json` | registro de población |
| `village/index.json` + `village/villages/<uuid>.json` | índice y un fichero por aldea |
| `economy/state.json` | economías de asentamiento, cuentas, mercaderes, caravanas, rutas, contratos |
| `economy/stores.json` | almacenes con sus lotes y procedencia |
| `economy/history.json` | libro de movimientos y memoria comercial |
| `quest/quests.json` | misiones, campañas, enfriamientos, historia |
| `family/people.json` | personas, aristas, parejas, nacimientos, historias contadas |
| `family/families.json` | familias, hogares, linajes, aprendizajes, técnicas, herencias, reliquias, legados |

## Cómo

- **Sobre de versión:** esquema, formato, checksum; escritura segura (temporal + mover, el anterior como copia `.bak`); recuperación desde la copia si el principal está dañado; migraciones paso a paso.
- **Solo lo sucio:** cada sección dice si cambió (indicadores y contadores de revisión por sección); cada `saveIntervalTicks` (1200) se escriben como mucho `maxSavesPerTick` (2), por turnos; **nunca por tick**. Al apagar se escribe todo (`saveAll`).
- **Bloqueo:** una sección escrita por una versión más nueva (o sin migración) se bloquea: la sesión usa valores por defecto y **no sobrescribe** el fichero.
- **Compresión** opcional (`compressStorage`, gzip+base64 en el sobre).
- **Auditoría:** tras cargar, la genealogía se valida y los problemas se informan.

## Reinicio

Al arrancar: cargar todo, reanudar el reloj desde el cielo actual (sin saltar un día por redondeos), aplicar tiempo offline si está configurado; los NPCs restaurados vuelven a sus aldeas (`embodied`), reciben su cama como hogar si no tienen otro, y el mundo sigue.

Pruebas: `everythingSurvivesARestart`, `theWorldSurvivesARestart`, `villagesSurviveARestart`, `theEconomySurvivesARestart`, `questsSurviveARestart`, `familiesSurviveARestart`, `LivingWorldTest.endToEndTheWorldLivesPersistsAndComesBack`, GameTest de persistencia y dos ejecuciones consecutivas del servidor de GameTest sobre el mismo mundo.
