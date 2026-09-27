# Generation Engine

- Cada persona tiene una **generación**; los fundadores de una familia son la 1, sus hijos la 2… Al llegar un NPC sin familia, sus antepasados históricos ocupan las generaciones anteriores (`ancestorDepth` = 2: padres y abuelos, cuatro abuelos por las dos ramas).
- Un nacimiento pone al niño en la generación siguiente a la de sus padres; el primer miembro de una generación nueva publica `GenerationAdvancedEvent`.
- La **edad** sale siempre del calendario oficial (`AGE_ENGINE.md`), nunca de ticks.
- La sucesión puede dar peso a la antigüedad de generación (`generation` en las reglas).

Prueba: `birthsAdvanceGenerationsAndAgeComesFromTheCalendar`.
