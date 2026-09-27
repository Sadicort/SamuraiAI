# Daily Schedule Engine

**Código:** `living/village/schedules/DailyScheduleEngine.java`, `ScheduleTemplate.java`, `DayPlan.java`.

## Plantillas culturales integradas (`templates`)

| Cultura | Día |
| --- | --- |
| village | 04:30 WAKE, 05:00 PRAYER, 06:00 WORK, 12:00 EAT, 13:00 WORK, 17:00 MERCHANT, 18:30 SOCIAL, 20:00 EAT, 21:30 SLEEP |
| temple | 04:00 WAKE, 04:30 PRAYER, 06:00 MEDITATE, 07:00 WORK, 11:30 EAT, 12:30 WORK, 16:00 PRAYER, 17:30 MEDITATE, 19:00 EAT, 20:30 SLEEP |
| market | 06:00 WAKE, 06:30 EAT, 07:00 MERCHANT, 12:30 EAT, 13:30 MERCHANT, 18:00 SOCIAL, 20:00 EAT, 22:00 SLEEP |
| guard | 05:00 WAKE, 05:30 TRAINING, 07:00 GUARD, 12:00 EAT, 13:00 PATROL, 18:00 EAT, 19:00 SOCIAL, 21:00 SLEEP |

## Oficios (`professionSchedules`)

Desplazamientos (`shift`, minutos) y sustituciones (`swap=A>B`): campesino −30; pescador −60; cocinero −30 y MERCHANT→WORK; mercader WORK→MERCHANT; monje WORK→PRAYER, MERCHANT→MEDITATE, −30; guardia WORK→GUARD, MERCHANT→PATROL, PRAYER→TRAINING; samurái WORK→TRAINING, MERCHANT→PATROL, PRAYER→MEDITATE; leñador −30; minero −15; cazador −60 y MERCHANT→WORK; herrero +15; tejedor +15; herbolario −30; sanador MERCHANT→PRAYER. Los guardias del turno de noche rotan el día entero `nightShiftMinutes` (720).

## Otros modificadores

- **Estación:** los bloques de la mañana siguen al amanecer (`SunModel.sunriseShift`).
- **Eventos:** en festival, el bloque social de la tarde se adelanta y la hora de dormir se retrasa.
- **Persona:** desfase propio del ciudadano (±90 min).

`DayPlan.routineAt(minuto)`, `label()` (por qué difiere de la plantilla), caché por ciudadano y día. Usos (sesgo del scheduler y horas abstractas): `../living_world_engine/DAILY_ACTIVITY_ENGINE.md`.

Prueba: `schedulesDifferByProfessionSeasonFestivalAndPerson`.
