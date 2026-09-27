# Almacenamiento de memoria

Ver también `docs/phase3/PHASE3_PERSISTENCE.md`.
- Archivo `npc/<uuid>/memory.json` bajo `<mundo>/data/samuraiai/cognition/`.
- Sobre (`VersionedStore`): `format`, `domain`, `schemaVersion`, `encoding` (plain | gzip-base64), `checksum` SHA-256, `savedAt`, `payload`.
- Escritura segura (temporal + `.bak` + movimiento atómico); lectura verifica checksum, restaura del `.bak` si hay corrupción, migra versiones antiguas paso a paso y **rechaza sin tocar** un archivo de versión futura (`TOO_NEW`) o sin ruta de migración (`UNSUPPORTED`).
- `MemoryCodec`: tolerante; un recuerdo ilegible se omite, no el archivo.
- Esquema actual: **1** (`MemoryStorage.SCHEMA`). Preparado para SQLite: cada `*Storage` es la costura.
