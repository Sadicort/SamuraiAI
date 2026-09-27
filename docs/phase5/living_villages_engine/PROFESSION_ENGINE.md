# Profession Engine (aldea)

**Código:** `living/village/professions/ProfessionAssigner.java`; `VillageEngine.assignProfession`, `professionCounts`.

El catálogo de oficios es del World Engine (`../living_world_engine/PROFESSION_ENGINE.md`); la aldea decide **quién** tiene cuál:

1. El **tipo de NPC** manda (un NPC de tipo guardia guarda).
2. Si no, lo que más necesita la **economía** de la aldea (`mostNeededProfession`: campesino cuando falta comida, leñador cuando falta leña…).
3. Si no, las **cuotas** de la aldea: 30 % campesinos, 1/8 guardias, 1/12 leñadores y pescadores (pescador solo con muelle o ≥ 12 habitantes), 1/15 cocineros y mercaderes (mercader solo con mercado), 1/20 herreros, carpinteros, monjes (solo con templo) y cazadores, 1/25 herbolarios, tejedores y mineros (solo con mina). Se elige el oficio más por debajo de su cuota.

Nunca cambia un oficio que alguien ya tiene. `ProfessionAssignedEvent` → la familia lo registra (y lo usa para sugerir oficios a los jóvenes, sin copiarlo); si es mercader, la economía le da puesto.
