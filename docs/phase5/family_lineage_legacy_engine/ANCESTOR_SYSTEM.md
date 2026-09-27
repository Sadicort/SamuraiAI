# Ancestor System

- Un NPC que llega **sin familia** recibe una familia con antepasados: `ancestorDepth` = 2 generaciones (padres y los cuatro abuelos, por las dos ramas), como personas `HISTORICAL` ligeras con fechas de nacimiento coherentes (diferencia padre-hijo `parentAgeGap` + margen, nunca menos de `minParentAge`).
- Una persona sin padres registrados recibe un par de **padres históricos** al necesitarlo (así dos hermanos pueden compartirlos).
- Los antepasados **nunca se cargan como entidades** ni se simulan: son registros.
- Obon honra a los antepasados; las historias sobre ellos se enseñan a los jóvenes.

Prueba: `anNpcWithoutAFamilyGetsOneWithAncestors`.
