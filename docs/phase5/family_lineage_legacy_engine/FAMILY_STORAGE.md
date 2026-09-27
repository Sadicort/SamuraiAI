# Family Storage

**Código:** `living/family/persistence/FamilyStorage.java`.

Dos ficheros versionados, cada uno con su propio contador de revisión (solo se reescribe el que cambió):

- `family/people.json`: personas, aristas del grafo (padres, parejas con fechas), nacimientos.
- `family/families.json`: familias (con memoria, reputación, honor, relaciones), hogares, linajes, aprendizajes, técnicas, herencias, reliquias, legados, historias ya contadas.

Tras cargar, el **validador audita** la genealogía; los problemas se informan (`LivingMetrics.auditProblems`, `/samuraiai living family audit`) y **nunca se «arreglan» en silencio**.

Prueba: `familiesSurviveARestart`.
