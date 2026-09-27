# Fase 5 — Simulación offline

## Servidor apagado

`offlineMode` en `samuraiai-calendar.toml`:

| Valor | Comportamiento |
| --- | --- |
| 0 (por defecto) | el tiempo de Deiliora solo pasa con el servidor encendido |
| 1 | al arrancar, el tiempo real que estuvo apagado se convierte en tiempo de Deiliora (`offlineRealMinutesPerDay` = 20 minutos reales por día) |
| 2 | como 1, pero nunca más de `offlineMaxDays` (7) días |

El reloj guarda la hora real de cada guardado (`savedAtReal`); `LivingService.install` llama `applyOffline` tras cargar y reanudar el reloj.

## Cómo se pone al día el mundo

- **Calendario:** los días saltados se procesan como un salto largo: como mucho `maxDaysPerAdvance` (90) uno a uno; el resto se resume (`skippedDays`). Las celdas de clima muy atrasadas saltan al presente con un sorteo (`fastForwards`).
- **Día:** cada día procesado encola su trabajo (4 etapas), que se hace un día por cada 4 ticks.
- **Regiones:** no se simulan todas al arrancar; cada región se pone al día **cuando le toca** (sus pasos por LOD, o `catchUp` acotado a 48 pasos si está muy atrasada).
- **Familias:** envejecen por fecha (la edad es fecha − nacimiento, no ticks); los aprendizajes avanzan con los días saltados (hasta 30 por paso).
- **Economía:** producción, consumo y deterioro escalados por la duración de cada paso; caravanas avanzan por minutos.
- **Aldeas:** visitantes de como mucho los últimos 30 días; horas planificadas acreditadas.

## Determinismo

Los sorteos son funciones de (semilla del mundo, clave, paso): el mismo mundo puesto al día dos veces toma las mismas decisiones (clima, eventos, nombres). Los ids de entidades nuevas son UUID aleatorios, así que dos mundos distintos no coinciden en todo.

Prueba: `aRegionAbandonedForMonthsIsCaughtUpWhenAPlayerReturns`, `offlineTimeIsAppliedOnlyWhenConfigured`.
