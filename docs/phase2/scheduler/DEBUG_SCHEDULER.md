# Depuración del scheduler

`debug.SchedulerInspector` explica en líneas de texto; comandos `/samuraiai scheduler ...` (operadores):

- `status` — periodo, eventos activos, población, buckets, coste, contadores y último fallo.
- `inspect <npc>` — estilo de vida y turno, bucket, ánimo, rasgos, condición (energía/fatiga/foco/estrés/motivación), rutina (estado, capa, progreso, lugar, **por qué**), comportamientos de fondo, grupo/rol/formación, **pila de interrupciones** (política, causa, cuándo expira), los 6 mejores **candidatos con puntuación y razón**, cooldowns.
- `groups`, `zones` — grupos (líder, formación, alarma) y zonas (ocupación, propietario, alerta).
- `zone add|remove|claim`, `group create|add|disband|formation`, `experience <npc> <causa> <cantidad>`.
- `debug` — overlay de partículas para quien lo pide: **anillos de zona** (color por tipo, rojo en alerta), **dorado** destino de cada rutina y marcador sobre el NPC, **blanco** líder de grupo, **cian** puesto en formación.
- `metrics reset`.

`scheduler.debugLogging` imprime la traza de un fallo de evaluación.

## Verificado

`status`, `inspect`, `zone add/remove`, `zones` y `groups` responden en servidor real (`theTimelineAnnouncesPeriodsAndCommandsRespond`); `-PnavTrace` activa `SCHED_TRACE` (inspector cada 100 ticks) en las pruebas físicas, que fue lo que permitió diagnosticar los fallos de integración. **El overlay de partículas no se ha verificado visualmente.**
