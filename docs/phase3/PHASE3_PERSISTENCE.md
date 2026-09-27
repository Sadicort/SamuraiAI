# Persistencia

- Directorio: `<mundo>/data/samuraiai/cognition/` — `npc/<uuid>/{memory,relationships,emotions,knowledge,personality}.json`, `society/society.json`. Los nombres salen **sólo del UUID** (nunca del nombre del NPC) y `VersionedStore` rechaza rutas fuera de la raíz.
- **Versionado**: cada archivo lleva `schemaVersion` (hoy 1 en todos). `Migration(from)` se encadena `v1→v2→v3`; test de cadena. Versión futura → `TOO_NEW`, versión sin ruta → `UNSUPPORTED`: el archivo se deja **intacto**.
- **Integridad**: SHA-256 del payload; ante corrupción se restaura `.bak` y el archivo malo se pone en cuarentena (`.corrupt-<ts>`); si todo está corrupto el dominio carga vacío y el resto sigue (`loadFailures`).
- **Escritura**: temporal + copia `.bak` + movimiento atómico; opcional compresión gzip (`compressStorage`).
- **Estrategia**: *dirty flags* por runtime; barrido cada `saveIntervalTicks` con tope `maxSavesPerTick`; los cambios **urgentes** (trauma, traición, recuerdo crítico, cambio de ánimo…) se escriben sin esperar al barrido; al descargar un NPC se guarda; al parar el servidor `saveAll`. Un NPC ocioso no se reescribe (test). La decadencia temporal se recomputa desde marcas persistidas y **no** marca dirty.
- Borrado definitivo de un NPC: `erase` (memoria, relaciones de todos hacia él, archivos).
- Coste medido: 300 NPC → 1 500 archivos, ≈ 3 MB comprimido, ≈ 3,4 s en total (guardado síncrono; el barrido periódico lo reparte).
- **Test de reinicio** (`theServerCanRestartAndEveryPartOfTheNpcsHistoryContinues`): nuevo mundo, mismo disco; se verifican recuerdo protegido con su traza, relación y promesa, ánimo y trauma, conocimiento, evolución de personalidad, comunidad, rumores y línea temporal; y que el tiempo apagado cuenta al reanudar.
