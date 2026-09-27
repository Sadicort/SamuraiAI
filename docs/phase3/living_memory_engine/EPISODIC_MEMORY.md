# Memoria episódica

Un `MemoryRecord` de tipo `EPISODIC` (o `EMOTIONAL`/`SPATIAL`/`PROCEDURAL`/`TEMPORAL` según `MemoryClassifier`) es una experiencia concreta.

Campos: `id` (= id de la Experience), `npcId`, `type`, `category`, `episode` (CONVERSATION, TRAVEL, COMBAT, PATROL, MEDITATION, FRIENDSHIP, FEAR, DISCOVERY, MISSION, FESTIVAL, REST, OBSERVATION), `kind` (ExperienceKind), `stamp` (tiempo del mundo, fecha real, clima), `endTime`, `place` (dimensión, xyz, zona), `actor`, `target`, `entities`, `emotion`, `importance`, `confidence`, `duration`, `state`, `tags`, `context`, `origin`, `events`, `consequences`, `chapters`, `version`, y estado de vida (`strength`, `accessCount`, `repeatCount`, `lastAccess/Decay/Reinforced/Echo`, `protected`).

**Capítulos** (`Chapter`): un episodio largo o fusionado conserva sub-tramos (acotado por `maxChaptersPerRecord`).
**Estados**: `TEMPORARY → CONSOLIDATED → FADING → COMPRESSED → FORGOTTEN`.
