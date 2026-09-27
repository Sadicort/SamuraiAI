# World Memory Engine

La memoria del mundo vive en tres niveles, sin copias:

| Nivel | Dónde | Qué guarda |
| --- | --- | --- |
| Cronología objetiva | `living/calendar/timeline/WorldTimeline` | cada hecho fechado con sus ámbitos (`region:`, `settlement:`, `village:`, `family:`…) y su causa |
| Memoria rápida | contadores de `Region` (`FIRES, WARS, BATTLES, FESTIVALS, DISASTERS, HEROES, VISITS, CONSTRUCTIONS`) y de `Village` (`FESTIVALS, FIRES, WARS, ATTACKS_REPELLED, AID, BETRAYALS, CONSTRUCTIONS, ARRIVALS, DEPARTURES, VISITORS`) | cuántas veces pasó algo, para decisiones baratas |
| Memoria recordada | comunidades de Knowledge (historia comunitaria, leyendas del `WorldMemoryEngine` cognitivo) | lo que la gente recuerda, con testigos y olvido |

El World Engine escribe en la cronología las fundaciones (significancia 0,8), los eventos de mundo y los caminos a través de su puerto `Chronicle`; la memoria comunitaria de la aldea (`CommunityMemory`) cuenta y, por encima de `memorySignificance`, escribe en la cronología y en la comunidad de Knowledge (`rememberInCommunity`, como `HistoricalEvent` con la etiqueta `living`).

«La historia de esta región» es `timeline.of("region:<uuid>", n)`: `/samuraiai living world region` muestra la región y sus contadores.
