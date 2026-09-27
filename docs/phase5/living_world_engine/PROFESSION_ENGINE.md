# Profession Engine

**Código:** `living/world/professions/ProfessionCatalog.java`, `ProfessionDef.java`; asignación en `living/village/professions/ProfessionAssigner.java`; producción en `living/economy/production/RecipeCatalog.java`; herencia sugerida en `living/family/profession/ProfessionHeritage.java`; habilidad en `living/core/Skill.java`.

## Reparto de responsabilidades

| Pregunta | Dueño |
| --- | --- |
| Qué es una profesión (tareas, herramientas, lugares, rutina de trabajo, sesgo del día, tipos de NPC) | World (`ProfessionCatalog`) |
| Quién la tiene | Village (`Citizen.profession`, `ProfessionAssigner`) |
| Qué produce y qué consume | Economy (`Recipe` por profesión) |
| Qué oficio sugiere la familia a un hijo | Family (`ProfessionHeritage`, solo sugerencia) |

## Profesiones integradas

campesino (farmer), pescador, herrero, carpintero, mercader (`MERCHANT`), monje (`PRAYER`), guardia (`GUARD`), samurái (`TRAINING`), cocinero, leñador, minero, cazador, herbolario, tejedor, sanador (`future=true`: preparado, aún no produce). Cada línea: `blacksmith;name=Herrero;tasks=forjar,reparar,templar;tools=tools;locations=SMITHY;work=WORK;bias=WORK:25;types=blacksmith;xp=1.0`.

## Asignación

`ProfessionAssigner` elige al admitir un ciudadano sin oficio: primero la que corresponde a su tipo de NPC (`types`); si no, la que más necesita la economía de la aldea (`Economy.mostNeededProfession`, p. ej. campesino cuando falta comida); si no, la más alejada de las **cuotas** de la aldea (30 % campesinos, un guardia por cada 8, un cocinero por cada 15, monje solo con templo, mercader solo con mercado, pescador solo con muelle o ≥ 12 habitantes, minero solo con mina…). Nunca cambia un oficio que alguien ya tiene. La **afinidad de personalidad** (`Outside.affinity`, rasgos cognitivos: valor/disciplina/lealtad para guardia, espiritualidad/paciencia/empatía para monje…) la usa la Family Engine al sugerir oficio a los jóvenes (`ProfessionHeritage`). `ProfessionAssignedEvent` → la familia lo registra; si es mercader y hay economía, se le da puesto (`registerMerchant`).

## Experiencia

Las horas de trabajo (`Citizen.professionHours`) se acumulan de dos fuentes: horas **planificadas** en simulación abstracta y rutinas **reales** completadas (`RoutineCompletedEvent` de WORK/MERCHANT/GUARD/PATROL/TRAINING → `LivingWorld.routineCompleted`). `Skill.rank(horas)` (aprendiz → maestro) y `Skill.multiplier` escalan la producción económica.
