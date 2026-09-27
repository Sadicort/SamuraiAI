# Anniversary Engine

**Código:** `living/calendar/holidays/AnniversaryEngine.java`, `AnniversaryRecord.java`, evento `AnniversaryEvent`.

- Registro de fechas que merece la pena recordar cada año: fundación de una aldea, una victoria o derrota, un nacimiento, un evento familiar, algo que hizo un jugador. `subject` dice a qué pertenece (`village:<id>`, `family:<id>`, `npc:<uuid>`…).
- Índice por (mes, día): encontrar los aniversarios de hoy es una búsqueda en un mapa, sea cual sea el número de registros.
- Se anuncia desde el primer año cumplido: `AnniversaryEvent(id, tipo, sujeto, título, años)`.
- Capacidad `anniversaryMax` (50000).
- Quién registra hoy: el World Engine (fundación de cada asentamiento, `SETTLEMENT_FOUNDING`, a través de su puerto `Chronicle.anniversary`) y la Family Engine (cada nacimiento, `BIRTH`). El puerto de Family expone el mismo método para otros aniversarios familiares.
- `about(sujeto)` lista los aniversarios de algo.

Prueba: `anniversariesAreAnnouncedEveryYear`.
