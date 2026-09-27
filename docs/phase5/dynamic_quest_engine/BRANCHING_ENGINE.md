# Branching Engine — caminos

**Código:** `living/quest/branching/Path.java`, `BranchSpec.java`; `QuestEngine.paths/choose`.

Caminos: pacífico, violento, sigiloso, diplomático, espiritual, honorable (`PEACEFUL, VIOLENT, STEALTH, DIPLOMATIC, SPIRITUAL, HONOR`).

- Una plantilla ofrece los caminos que permite (`BranchSpec`: camino, etiqueta, confianza mínima del dador en el jugador, posición mínima del jugador en la aldea −1..1, factor de recompensa).
- `/samuraiai living quest paths <id>` lista los disponibles **para ti**; `/samuraiai living quest choose <id> <camino>` elige. Si solo hay uno disponible se elige solo al aceptar.
- Elegir camino activa los **objetivos de ese camino** (los de otros caminos se saltan), multiplica las recompensas y selecciona las **consecuencias** marcadas para ese camino.

Prueba: `pathsChangeObjectivesAndConsequences`.
