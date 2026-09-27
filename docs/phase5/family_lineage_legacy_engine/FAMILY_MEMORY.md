# Family Memory

**Código:** `living/family/family_memory/FamilyMemoryEntry.java`; `FamilyEngine.remember/tellStories`.

- La **memoria colectiva** de la familia: lo que la familia cuenta de sí misma — fundación, nacimientos, uniones, muertes, desapariciones, migraciones, guerras, traiciones, heroísmo, oficios, casas, propiedades perdidas o ganadas, sucesiones, reliquias, tradiciones, ramas.
- **No son recuerdos personales** (esos son del Memory Engine y **nunca** se copian a los hijos).
- **Historias contadas:** cuando alguien llega a joven adulto (o se conoce ya adulto), el pariente vivo de más edad le **cuenta** las historias familiares con significancia ≥ `storySignificance` (0,45) anteriores a él: la persona las **aprende como conocimiento** (capa cognitiva, experiencia `LEARNED_FROM` con el narrador), no las recuerda como vividas. Cada historia se cuenta una vez por persona.
- Obon: las familias con antepasados los honran (entrada `TRADITION`).
