# Métricas del calendario

**Código:** `living/calendar/metrics/CalendarMetrics.java` (contadores `AtomicLong` + `Snapshot`), mostradas por `CalendarInspector.metrics` y `/samuraiai living metrics`.

- Volumen: ticks, días, meses, años, estaciones, cambios de clima, festivales, festivos, aniversarios, cosechas anunciadas.
- Saltos: días resumidos (`skippedDays`), minutos manuales (comandos), minutos offline.
- Integridad: retrocesos rechazados (`rewindsRefused`), realineaciones con el sol, `fastForwards` de celdas de clima muy atrasadas.
- Coste: tiempo medio y máximo por tick y por día procesado (`day(nanos)`).
- Tamaño: entradas de la cronología, entradas descartadas, aniversarios, celdas de clima.

El hub añade el tiempo del calendario por tick a `LivingMetrics.calendarNanos`. Ver `../PHASE5_PERFORMANCE.md`.
