# Genealogy Engine

**Código:** `living/family/genealogy/GenealogyGraph.java`, `ConsistencyValidator.java`.

- **Dos aristas canónicas:** `PARENT_OF` (dirigida) y `PARTNER_OF` (no dirigida, con el minuto en que empezó y su fin). Hijos, hermanos, abuelos, primos, ancestros y descendientes **se derivan**: el parentesco no puede registrarse dos veces ni contradecirse. Tampoco se duplica en el Relationship Engine.
- Conjuntos de ancestros en caché por persona, invalidados por un contador de versión al añadir o quitar aristas.
- `addParent`, `addPartners` pasan por el validador (`FAMILY_CONSISTENCY_VALIDATOR.md`).
- `ancestors(p, profundidad)`, `descendants`, `siblingsOf`, `grandparentsOf`, `currentPartners`, `related`, `degree`, `commonAncestor`.

Árbol visible: `/samuraiai living family tree <npc>` (`GENEALOGY_DEBUGGER.md`).
