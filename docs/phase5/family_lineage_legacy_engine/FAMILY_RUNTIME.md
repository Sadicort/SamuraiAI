# Family Runtime

**Código:** `living/family/registry/FamilyRecord.java`.

`FamilyRecord` es a la vez el registro y el estado vivo de una familia: nombre, fundadores, cuándo y dónde empezó, estado (`ACTIVE, DECLINING` con un solo miembro vivo, `EXTINCT`, `DISPERSED`, `MIGRATED`, `HISTORICAL`), generaciones, ramas, **reputación** y **honor** con sus causas (`CauseLedger`), importancia histórica, jefe y sucesor designado, hogares, tradiciones, conocimiento que guarda (técnicas e historias), reliquias, propiedades, relaciones con otras familias (`ALLY, FRIENDLY, NEUTRAL, RIVAL, HOSTILE, MASTER_LINEAGE, TRADE_PARTNER`) y memoria colectiva, aldea y etiquetas (p. ej. `culture:temple`).

Los **miembros no se guardan dos veces**: se encuentran por el índice de familia del motor (`members(familia)`, `living(familia)`).

`/samuraiai living family info <npc>` (op).
