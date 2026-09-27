# Lineage Engine

**Código:** `living/family/lineage/Lineage.java`; `FamilyEngine.createLineage/lineageSuccession`.

- Tipos: `FAMILY, SAMURAI, CRAFT, RELIGIOUS, MERCHANT, SCHOLAR, CUSTOM`.
- **Linaje biológico y linaje marcial son cosas distintas:** una escuela samurái la llevan maestros y discípulos, no la sangre.
- Guarda fundador, líderes a lo largo del tiempo (cada generación de la escuela), miembros, conocimiento (técnicas), tradiciones, reputación, honor, oficio, escuela (dojo o taller), reliquias, historia y estado (`ACTIVE, DORMANT`…). El fundador lleva el nombre del linaje en su `NameRecord`.
- **Sucesión de escuela:** si el líder ya no está, lo sucede el discípulo con más progreso (los que completaron primero); si no hay, otro miembro vivo; si nadie, la escuela queda **dormida**. `LineageLeaderChangedEvent`.
- Creación: `/samuraiai living family lineage create <tipo> <fundador> <nombre>` (op). Los aprendizajes que empieza un líder por comando quedan dentro de su escuela.

Prueba: `mastersTeachDisciplesAndSchoolsOutliveFounders`. Árbol de escuela: `/samuraiai living family lineage <nombre>`.
