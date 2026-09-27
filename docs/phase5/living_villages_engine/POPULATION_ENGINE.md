# Population Engine (aldea)

**Código:** `living/village/population/VillageCensus.java`; `VillageEngine.census`.

`VillageCensus`: residentes, ciudadanos sin entidad, reparto por oficio, guardias, monjes, niños y ancianos (según las etapas de vida de Family), visitantes presentes y sin techo. Se calcula al vuelo.

Cuando cambia la población se publica `VillagePopulationChangedEvent(población, oficios)`; el hub la pasa al registro del mundo (`../living_world_engine/POPULATION_ENGINE.md`) y la economía recalcula consumo y necesidades con ella. El censo alimenta además el escáner de misiones (p. ej. faltan guardias si hay ≥ 6 residentes, menos de `guardRatio` guardias y la aldea no está en paz).
