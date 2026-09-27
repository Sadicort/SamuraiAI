# Métricas del scheduler

`metrics.SchedulerMetrics.snapshot()`: evaluaciones (nº, µs medios y máximos), ticks (µs medios y máximos), rutinas iniciadas/completadas/interrumpidas/reanudadas/caducadas, conflictos resueltos, cambios de líder, de periodo y de ánimo, emergencias, evaluaciones aplazadas, cambios de personalidad, alarmas, **fallos** (y el último mensaje), y población: NPC, grupos, zonas, escala de multitud y reparto por bucket.

Preguntas rápidas: ¿cuánto cuesta? → `averageTickMicros`, `maxTickMicros`; ¿limita la cuota? → `deferred`; ¿algo falla? → `failures` + `lastFailure`; ¿hay vaivén? → `routinesStarted` frente al tiempo.

Consulta: `/samuraiai scheduler status` y `/samuraiai scheduler metrics reset` (verificado en servidor real).
