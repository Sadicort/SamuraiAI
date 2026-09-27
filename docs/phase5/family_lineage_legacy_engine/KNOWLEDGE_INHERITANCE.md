# Knowledge Inheritance — el saber que se enseña o se pierde

**Código:** `living/family/knowledge/Technique.java`; `FamilyEngine.technique/teach`.

- Una **técnica** es saber que debe enseñarse para sobrevivir: una técnica de espada, un secreto de forja, un sutra, una receta, una historia familiar. Registra creador y fecha, linaje o familia que la guarda, rareza, **quién la sabe ahora** (poseedores vivos) y cada transmisión (de quién a quién, cuándo, cómo: `PARENT, MASTER, ORAL, COMMUNITY, PROFESSION`; `BOOK_FUTURE` preparado).
- **Enseñar** (`teach`): solo quien la sabe puede enseñarla; el alumno la **aprende en la capa cognitiva** (experiencia `LEARNED_FROM`, y el maestro `TAUGHT`), y la familia del alumno la añade a su conocimiento. `TechniqueTaughtEvent`.
- **Perder:** cuando el último poseedor se va (muere, desaparece, pasa a la historia) sin haberla enseñado, la técnica se **pierde** (`TechniqueLostEvent`), pero su **evidencia** queda («solo quedan historias sobre …»), en la memoria familiar y en la cronología.
- Nunca se copia el conocimiento de un padre a un hijo: se enseña (maestro, padre, comunidad) o se pierde.

Comandos (op): `family technique <npc> <clave> <nombre>`, `family teach <clave> <de> <a>`. Prueba: `untaughtKnowledgeIsLostButItsEvidenceRemains`.
