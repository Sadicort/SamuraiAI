# Family, Lineage & Legacy Engine (Fase 5.5)

**Código:** `living/family/**` (motor puro), `FamilyEngine`, `FamilySettings` (`samuraiai-family.toml`), puertos `integration/FamilyPorts`.

## Pregunta

«¿De dónde viene este NPC y qué dejará?». Posee familias, parentesco (un grafo con dos aristas canónicas), generaciones, hogares (*households*), jefes de familia y sucesión, linajes (familias, escuelas samurái, oficios, líneas religiosas y mercantiles), maestros y discípulos, el conocimiento que guardan (y pierden), herencias, reliquias, reputación y honor familiares con sus causas, memoria e historia familiar, tradiciones, nombres, edades, estados de vida y legado.

## Lo que no es

- **No es el Relationship Engine:** que alguien sea tu padre es parentesco; si os queréis es de las relaciones cognitivas. **Familia no implica relación positiva.**
- **No es el Memory Engine:** un hijo nunca recuerda lo que pasó antes de nacer; las historias familiares se le **enseñan** como conocimiento (`FAMILY_MEMORY.md`). Los recuerdos de los padres no se copian.
- **No es la Economy:** pide monedas y edificios a través de puertos; no los guarda.
- **No copia** profesión ni reputación: la profesión se sugiere (`PROFESSION_HERITAGE.md`) y la reputación familiar es una expectativa, no la reputación personal (`REPUTATION_HERITAGE.md`).

## Ciclo

- **Llegada de un NPC** (`CitizenJoinedEvent` → `adopt`): si no tenía registro, recibe persona, familia (con antepasados históricos) y hogar.
- **Diario** (`simulate`, desde el hub al empezar cada día, con los días saltados): etapas de vida, historias contadas a quien llega a joven adulto, aprendizajes, hogares, sucesión si falta jefe, tradiciones, extinción. **Nunca recorre toda la genealogía por tick.**
- **Sucesos:** muerte real → sucesión, herencia, legado, técnicas perdidas; nacimiento registrado; Obon → honrar antepasados.

Documentos: ver el índice en `../README.md`. Pruebas: `FamilyEngineTest` (12) y `LivingWorldTest`.
