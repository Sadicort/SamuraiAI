# Master–Disciple Engine

**Código:** `living/family/mentorship/Mentorship.java`; `FamilyEngine.mentor/train/endMentorship`.

- Registro: maestro, discípulo, tipo (`CRAFT, SAMURAI, RELIGIOUS, MERCHANT, SCHOLAR, CUSTOM`), inicio y fin, técnicas y conocimiento enseñados, progreso 0..1, confianza y respeto del discípulo hacia el maestro (medidos de las relaciones cognitivas; las reales siguen allí), estado y peso en el legado del maestro.
- Tubería: `CANDIDATE → MASTER → SKILL → TRAINING → KNOWLEDGE → EXPERIENCE → COMPLETION`.
- **Entrenar** (cada día): solo si maestro y discípulo viven en la misma aldea; el progreso sube `mentorDailyRate` (0,01) × (0,5 + calidad de enseñanza) por día. La **calidad** sale de la capa cognitiva: respeto y confianza del discípulo, paciencia del maestro, diligencia del discípulo (`Outside.teachingQuality`).
- Las técnicas del maestro se van enseñando al alcanzar umbrales de progreso (`KNOWLEDGE_INHERITANCE.md`).
- **Fin:** completado (el discípulo lleva «discípulo de …», toma el nombre de la escuela y, si era un aprendizaje de oficio y no tenía oficio, el del maestro), maestro fallecido, roto, discípulo marchado.
- **Cómo empieza:** misión «{maestro} busca un discípulo» (condición `MENTOR_WANTED`: un maestro maduro con una técnica que nadie aprende y un joven adulto en la aldea) con la consecuencia `MENTORSHIP`, o `/samuraiai living family mentor <maestro> <discípulo> <tipo>` (op).
