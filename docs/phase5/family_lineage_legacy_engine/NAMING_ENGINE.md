# Naming Engine

**Código:** `living/family/naming/NamingEngine.java`, `NameRecord.java`.

> Extendido por la Fase 5.5 (identidad, clanes y ancianos): `NameRecord` ganó epíteto, cultura y orden de lectura; `NamingEngine` ahora reparte la generación entre cinco culturas de nombre en vez de solo yamato. Ver `DARK_FANTASY_NAME_ENGINE.md` y `CULTURAL_NAMING_PROFILES.md` — este documento describe el comportamiento original, que sigue vigente para yamato.

- Nombre en partes: nombre, apellido, honorífico, título, nombre del linaje, epíteto, cultura y orden. En yamato el apellido va primero («Takeda Hiro»); en las cuatro culturas de fantasía oscura añadidas, el nombre de pila va primero («Aldren Ashborne»).
- Un NPC existente con dos palabras en el nombre se divide en apellido y nombre; con una, se le da un apellido de familia (salvo que sea un nombre genérico de repuesto — ver `NPC_CREATOR_NAMING_INTEGRATION.md`).
- Nombres nuevos (antepasados, nacimientos sin nombre) inspirados en la onomástica japonesa o, según la cultura de la familia, en la fantasía oscura medieval y gótica; siempre **deterministas** (`Dice`): el mismo mundo repetido nombra igual a su gente.
- El nombre **no** se usa para adivinar el género (`KinGender` queda `UNSPECIFIED` salvo que se registre).
