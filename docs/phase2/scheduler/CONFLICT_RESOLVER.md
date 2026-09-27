# Conflict Resolver

Clase `conflict.ConflictResolver` (+ `Resolution`). Resuelve las **demandas simultáneas** sobre un NPC: su rutina, una orden de grupo, una regla de zona, una respuesta a un evento, un tirón emocional, una necesidad.

## Regla (fija y explicable)

1. **Capa** más alta gana sin discusión.
2. Dentro de la capa, mayor **puntuación**, salvo que estén a menos de `switchMargin/2` (4): entonces gana la fuente con más **autoridad** (`Source`: ROUTINE 1 < NEED/EMOTION 2 < ZONE 3 < GROUP 4 < EVENT 5 < SURVIVAL 6).
3. Empate final: por nombre (determinista).

Cada perdedor se devuelve con su **motivo** ("layer SITUATIONAL outranks BASELINE", "score 57 beats 50", "GROUP is more authoritative than ROUTINE", "tie broken by name"). Cuando hay conflicto real y cambia el ganador se publica `BehaviorConflictResolvedEvent(winner, source, losers)` y se cuenta en las métricas.

Pruebas: `theResolverExplainsWhyEachLoserLost`.
