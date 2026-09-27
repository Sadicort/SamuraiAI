# Aldeas en el mundo vivo (resumen)

El detalle completo de 5.1 está en `../living_villages_engine/` (20 documentos). Aquí, cómo encajan las aldeas en el mundo:

- **Aldea = asentamiento.** El id de la aldea es el id del asentamiento del World Engine; el World sabe *que existe y dónde*, el Village Engine lo que pasa *dentro* (ciudadanos, edificios, distritos, horarios, seguridad), la Economy lo que *produce y guarda*. Nadie duplica al otro.
- **Fundación:** `SettlementFoundedEvent` → el hub crea la aldea (con plano de distritos y edificios planificados si se pidió) y su economía (con mercado y/o templo según el tipo y los edificios).
- **Aldeas automáticas:** un NPC que aparece donde no hay aldea funda una nueva (`autoVillages`, `autoVillageRadius`, nombre «<Región> no Sato»), o se une a la más cercana en `villageJoinRadius`.
- **Comunidad:** cada aldea enlaza una comunidad de Knowledge (clave `communityKey`) donde viven el conocimiento colectivo, la cultura, la historia recordada y la reputación.
- **Población:** la aldea informa al World (`VillagePopulationChangedEvent` → `reportPopulation`).
- **Simulación:** la aldea avanza como simulador de su región (`SIMULATION_ENGINE.md`).
