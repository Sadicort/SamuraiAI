# Quest Generator

**Código:** `living/quest/generator/QuestGenerator.java`, `conditions/WorldCondition.java`, `ConditionKind.java`.

## Condiciones (`WorldCondition`)

Tipo, **clave estable** (`tipo:asentamiento:sujeto`, así el mismo problema informado dos veces nunca crea dos misiones), asentamiento, región, sujeto (recurso, persona, camino), severidad 0..1, causa (`Provenance`) y variables (`resource`, `quantity`, `eventId`, `x`, `z`, `giver`…).

| Condición | Quién la informa |
| --- | --- |
| SCARCITY, WORKSHOP_STARVED | hub, al empezar una escasez (taller si un oficio de la aldea necesita ese recurso como entrada) |
| LOST_CARAVAN, ROUTE_BLOCKED | hub, eventos de caravana y de ruta |
| BANDITS, ATTACK, WAR, FIRE | hub, al empezar el evento de mundo |
| HOUSING_SHORTAGE | hub, `HousingShortageEvent` |
| GUARD_SHORTAGE | escáner diario: aldea amenazada con ≥ 6 residentes y menos de `guardRatio` guardias |
| FESTIVAL_SOON | escáner: festival de su cultura a ≤ `festivalWarningDays` (3) días |
| TEMPLE_RITUAL | escáner: aldea con templo a ≤ `ritualWarningDays` (2) días de luna llena |
| GRATITUDE | escáner: miembro de la comunidad con gratitud (emoción ≥ 40) y afinidad > 65 hacia un jugador |
| EXPLORATION | escáner semanal: región vecina sin asentamientos ni jugadores en 30 días |
| MISSING_PERSON | hub: un miembro de una familia pasa a desaparecido y hay un pariente en la aldea |
| HEIRLOOM_LOST, FAMILY_DISHONOR, MENTOR_WANTED | escáner de familias: reliquia perdida, honor < −5, maestro maduro cuya técnica nadie aprende y un joven adulto en la aldea |
| DISPUTE | escáner: dos vecinos con rivalidad fuerte en sus relaciones cognitivas (rivalidad ≥ 60, o confianza ≤ 15 y respeto ≤ 25), una por aldea y día |
| GRUDGE | escáner de familias: dos familias rivales u hostiles en la misma aldea. Una disputa entre miembros de dos familias vista `FEUD_DAYS` (3) días vuelve **rivales** a las familias |
| CUSTOM | misiones encadenadas (seguimientos) |

`conditionResolved(clave)`: si el problema desaparece solo (termina la escasez, se reabre la ruta, llega la caravana, termina el evento), la misión abierta pasa a `RESOLVED_BY_WORLD`: el mundo lo resolvió antes que el jugador.

## Generación

1. Plantillas cuyo disparador es esa condición, filtradas por **estación** y **cultura** de la aldea; elección por peso y severidad (`Dice` determinista).
2. **Variables** de la condición y del mundo: asentamiento, región, recurso y su precio, estación (en español), clima, luna, vecinos, lugares (edificio, centro de región, punto de un evento o de un camino).
3. **Quien la da:** un ciudadano presente con uno de los oficios que pide la plantilla (o la persona de la condición). **Sin voz no hay misión**: si nadie puede darla, no se crea.
4. Los objetivos se colocan en el mundo (`OBJECTIVE_ENGINE.md`).
5. `QuestCreatedEvent`; el adaptador avisa a los jugadores cerca de la aldea (`offerRadius`).
