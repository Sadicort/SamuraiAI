# Rumores (visto desde Relationship)

El rumor vive en el motor de conocimiento (`docs/phase3/knowledge_society_engine/RUMOR_ENGINE.md`). Relationship lo recibe como `ReputationHearsay` (sujeto, etiqueta, fuerza, ámbito, fuente, id del rumor) y responde actualizando el libro de reputación y, con influencia limitada, la relación con el sujeto. Todo rumor conoce su procedencia. `RumorCreatedEvent` lo publica Knowledge.
