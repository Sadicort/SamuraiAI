# Almacenamiento

NPC: `npc/<uuid>/knowledge.json` (esquema 1). Sociedad: `society/society.json` (comunidades con su conocimiento colectivo, historia, tradiciones, posiciones, rumores con saltos y transformaciones, leyendas). Misma política de versión/checksum/escritura segura/migración. La cola de propagación es transitoria (no se persiste); la línea temporal se reconstruye de las historias de comunidad.
