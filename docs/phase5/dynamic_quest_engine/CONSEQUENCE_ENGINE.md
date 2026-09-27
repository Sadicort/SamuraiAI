# Consequence Engine

**Código:** `living/quest/consequences/ConsequenceSpec.java`; `QuestEngine.applyConsequences`; implementación en los puertos del hub.

Cada consecuencia dice si se aplica al tener éxito, al fracasar o ambas, y si se limita a un camino. Llega al motor dueño de lo que cambia:

| Consecuencia | Motor | Efecto |
| --- | --- | --- |
| RENOWN / UNREST | Village | renombre / malestar de la aldea |
| REPUTATION | Knowledge | posición del jugador en un contexto |
| RELATIONSHIP | capa cognitiva | experiencia del dador (ayuda, traición) |
| MEMORY | capa cognitiva | los testigos de la aldea lo recuerdan |
| HISTORY | Calendar | entrada en la cronología |
| RESOLVE_EVENT / SPAWN_EVENT | World | termina o empieza un evento de mundo |
| LOOT | Economy | la aldea pierde parte de sus almacenes |
| BUILD_HOUSE / BUILD_ROAD | Village / World | la aldea registra una casa; la red de caminos gana un tramo |
| FAMILY_HONOR / FAMILY_MEMORY / MENTORSHIP | Family | honor, historia familiar, un maestro toma discípulo |

**Fracasar también tiene consecuencias:** fallar, abandonar o dejar caducar una misión aceptada aplica las consecuencias de fracaso; una misión que **nadie** aceptó y caduca también las aplica (el problema sigue su curso), salvo las encadenadas. `QuestConsequenceAppliedEvent` por cada una.
