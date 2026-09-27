# Community Memory Engine

**Código:** `living/village/memory/CommunityMemory.java`; `VillageEngine.remember`.

Lo que una aldea ha vivido: festivales, incendios, guerras, ataques rechazados, ayuda recibida, traiciones, construcciones, llegadas y partidas, visitas.

1. **Contadores** propios de la aldea (`Village.Counter`), su memoria rápida.
2. **Cronología:** cada entrada va a la cronología mundial con los ámbitos de la aldea (`village:<id>`, `settlement:<id>`, `region:<id>`): la «línea de memoria de la aldea» persistente es una vista de esa única cronología.
3. **Comunidad de Knowledge:** si la significancia llega a `memorySignificance` (0,5), la aldea lo recuerda en la historia de su comunidad (`rememberInCommunity`), así sus miembros lo recuerdan juntos y pueden contarlo (rumores, conversaciones).

Además el **renombre** de la aldea sube con festivales (+0,5), ataques rechazados (+3) y las consecuencias `RENOWN` de las misiones (que pueden ser negativas si se fallan); sus causas se guardan.
