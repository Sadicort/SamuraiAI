# Changelog 3.2 — Relationship Engine

Añadido `ai.relationship` (≈ 55 clases). El antiguo `npc.relationship.RelationshipService` (5 ejes, sin persistencia) se **conserva** y recibe una proyección de la relación cognitiva (`CognitionService.projectRelationship`) para que diálogo y prompts la lean. Tests: `RelationshipEngineTest` (19).
Bugs de tests: umbral de nivel UNKNOWN tras una traición; nivel de confianza esperado vs. tramos.
