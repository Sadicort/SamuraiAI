# Succession Engine

**Código:** `living/family/succession/SuccessionEngine.java`; `FamilyEngine.succeed/successionRanking`.

Tubería: **JEFE NO DISPONIBLE → REGLAS → CANDIDATOS → EVALUACIÓN → SUCESOR → EVENTO → ACTUALIZACIÓN**.

- Candidatos: miembros vivos adultos (joven adulto o más) distintos del jefe anterior.
- Reglas **por cultura** de la aldea de la familia (`successionRules` configurable):

| Cultura | edad | generación | reputación | honor | oficio | conocimiento | designación | tradición |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| village | 1,0 | 1,0 | 0,5 | 0,3 | 0,5 | 0,4 | 3,0 | 0,8 |
| temple | 0,5 | 0,5 | 0,5 | 0,5 | 0,2 | 1,5 | 3,0 | 0,5 |
| guard | 0,5 | 0,5 | 0,6 | 1,5 | 0,6 | 0,6 | 3,0 | 0,5 |
| market | 0,6 | 0,6 | 1,2 | 0,3 | 1,0 | 0,4 | 3,0 | 0,6 |

- **No hay regla patriarcal ni matriarcal universal: el género no es una entrada.**
- Cada candidato recibe una puntuación con sus **razones** (`/samuraiai living family succession <npc>`).
- Sin candidatos adultos la familia queda sin jefe (y se comprueba si se extingue).

Prueba: `deathTriggersSuccessionInheritanceAndLegacy`.
