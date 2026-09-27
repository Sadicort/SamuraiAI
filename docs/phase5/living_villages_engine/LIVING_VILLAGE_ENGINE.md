# Living Villages Engine (Fase 5.1)

**Código:** `living/village/**`. Motor puro (sin Minecraft); integración en el hub `living/sim` y el adaptador `living/server`.

## Responsabilidad

«Cómo funciona esta comunidad»: aldeas, ciudadanía, edificios, distritos, casas y camas, oficios locales, horarios diarios, vida de mercado y templo, guardia, visitantes, seguridad, eventos de aldea y memoria comunitaria. Las aldeas **siguen funcionando sin jugadores**: se simulan con las regiones del mundo.

**Nunca mueve un NPC.** Sus planes del día, turnos y eventos llegan al Behavior Scheduler como **sesgo de rutinas** (`routineBias`, ver `../living_world_engine/NPC_SCHEDULE_ENGINE.md`); el scheduler sigue decidiendo.

## Lo que no es suyo

| Dato | Dueño |
| --- | --- |
| Que el asentamiento existe, dónde, región | World (el id de la aldea **es** el id del asentamiento) |
| Mercancías, precios, tesoros | Economy |
| Edad, familia, hogar familiar (household) | Family |
| Relaciones, emociones, recuerdos | capa cognitiva (Fase 3) |
| Conocimiento colectivo, cultura, reputación, historia recordada | comunidad de Knowledge enlazada |

## Documentos

`VILLAGE_RUNTIME.md`, `DISTRICT_ENGINE.md`, `BUILDING_ENGINE.md`, `HOME_ENGINE.md`, `CITIZEN_ENGINE.md`, `POPULATION_ENGINE.md`, `DAILY_SCHEDULE_ENGINE.md`, `PROFESSION_ENGINE.md`, `SOCIAL_LIFE_ENGINE.md`, `MARKET_ENGINE.md`, `TEMPLE_ENGINE.md`, `GUARD_ENGINE.md`, `COMMUNITY_MEMORY_ENGINE.md`, `VILLAGE_EVENTS.md`, `VISITOR_ENGINE.md`, `SECURITY_ENGINE.md`, `PERSISTENCE_ENGINE.md`, `DEBUG_VILLAGE_OVERLAY.md`, `PHASE5_1_CHANGELOG.md`.

## Ciclo

- **Tick** (bucket de aldeas del hub, cada `tickIntervalTicks` = 20, como mucho `villagesPerTick` = 8 aldeas): caducan eventos, se abren los festivales del día, lluvia, quién hace qué ahora (plan cacheado), mercado, templo, guardia, vida social, actividad de distritos, seguridad, censo y malestar.
- **Paso de región** (`simulate(aldea, desde, hasta, fullDetail)`): visitantes del periodo; si no hay detalle completo, se acreditan las horas de trabajo planificadas como horas de oficio.
- **Llegada/partida** de ciudadanos (`admit`/`depart`) desde el adaptador y el hub.

Configuración: `samuraiai-village.toml`. Pruebas: `VillageEngineTest` (9) + GameTests del mundo vivo.
