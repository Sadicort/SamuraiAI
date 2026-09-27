# Almacenamiento de relaciones

`npc/<uuid>/relationships.json`, esquema 1: relaciones (ejes, causas por eje, historial, enlaces), promesas, libro de reputación. Igual política que memoria (versión, checksum, escritura segura, migraciones). Enfriamiento y decaimiento **no marcan dirty** (se recomputan desde marcas de tiempo persistidas).
