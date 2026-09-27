# Métricas de percepción

`metrics.PerceptionMetrics.snapshot()`: pases, µs medios y máximos por pase, raycasts usados y rechazados, objetivos evaluados, objetos vistos, pérdidas de visión, sonidos oídos, estímulos (crudos/aceptados/rechazados), amenazas, curiosidad, sospechas levantadas, cambios de conciencia, fallos de sensor, recuerdos olvidados, NPC aplazados, NPC seguidos, µs medios y máximos por tick de servicio, y escaneos por tipo de sensor.

Preguntas rápidas: ¿cuesta mucho? → `averagePassMicros`, `averageTickMicros`; ¿se agota el presupuesto? → `raysRefused`, `npcsDeferred`; ¿algún sensor falla? → `sensorFailures`.

Consulta: `/samuraiai perception status`, `/samuraiai perception metrics reset`.
