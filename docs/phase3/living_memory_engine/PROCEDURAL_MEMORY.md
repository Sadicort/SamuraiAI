# Memoria procedimental

`procedural/ProceduralMemory`: `Skill(key, SkillKind ROUTE|PATROL|SAFE_PLACE|REST_SPOT|MEDITATION|TRAINING|DOOR|PATH, waypoints ≤16, proficiency, uses, lastUsed, protected)`.

- Un experience con `context.skill` la practica (`practiceGain`): `proficiency += gain·(1−proficiency)`.
- Con `protectSkillUses` usos la habilidad queda protegida.
- Se oxida lentamente si no se usa (`rust`). Persistida.
