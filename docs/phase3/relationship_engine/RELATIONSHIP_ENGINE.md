# Relationship Engine

`ai.relationship`. Responde **¿qué pienso de esta persona?**. `RelationshipEngine` posee un `RelationshipRuntime` por NPC (relaciones por UUID del otro, promesas, libro de reputación) y el `SocialGraph` global.

`RelationshipRecord`: id, origen, destino (`EntityRef`), tipo, estado, **7 ejes 0-100** (TRUST, RESPECT, AFFINITY, FEAR, LOYALTY, RIVALRY, HONOR), historial, recuerdos y trazas asociados, **causas por eje** (`CauseLog`), etapa de amistad, tipo de lealtad, contadores (interacciones, positivas/negativas, juramentos cumplidos/rotos, peligro compartido), versión. **Direccional**: la relación inversa es otro registro.

`apply(SocialEvidence)`: cada eje → `DimensionEngine.modulate` (holgura, reglas de personalidad, humor acotado, regla propia del eje). Nunca lee memoria: el hub le pasa evidencia.
