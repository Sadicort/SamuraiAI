# Métricas de navegación

Clases: `metrics.NavigationMetrics` (globales, atómicos), `SessionMetrics` (por sesión). Instantánea: `NavigationMetrics.snapshot()`.

## Por sesión

ticks, distancia, coste, nodos expandidos, tiempo de búsqueda, tiempo de movimiento, recálculos, bloqueos, obstáculos, puertas abiertas/cerradas, saltos, chunks cruzados, atascos, recuperaciones, acierto de caché.

## Globales

Peticiones, rutas creadas, aciertos de caché, completadas, falladas (**por motivo**), canceladas, recálculos, bloqueos, atascos, recuperaciones, obstáculos, puertas, saltos, nodos expandidos, ms de búsqueda y de movimiento,
chunks, distancia y coste totales, sesiones activas, coste medio y máximo del tick de navegación (µs), veces que se agotó el presupuesto de búsqueda.

## Respuestas rápidas

- ¿Cuánto tarda navegación? → `averageTickMicros` / `maxTickMicros`, `searchMillis`, `movementMillis`.
- ¿Cuántas rutas se recalculan? → `recalculations`; ¿por qué fallan? → `failuresByReason`.
- ¿Limita el presupuesto? → `budgetExhausted` (el test de 25 caminantes lo exige > 0 con un presupuesto pequeño).
- ¿Funciona la caché? → `PathCache.stats().hitRate()`.

## Consulta

`/samuraiai nav status` (resumen), `/samuraiai nav inspect <npc>` (sesión), `/samuraiai nav metrics reset`. Verificado en servidor real (`navigationCommandsAndInspectorRespond`).
