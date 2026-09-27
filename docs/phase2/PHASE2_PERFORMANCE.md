# Fase 2 — Rendimiento

Todo tiene presupuesto, intervalo y métrica propios; nada corre "todos contra todos cada tick".

| Motor | Mecanismo | Valores por defecto |
| --- | --- | --- |
| Navigation | A* **reanudable** con presupuesto de nodos por tick compartido entre caminantes; grafo perezoso por chunk con caché e invalidación; caché de rutas LRU con reutilización de sufijo; frecuencia por tier (lejos ×4, se reaplica la última orden) | 600 nodos/tick, 6000 máx. por búsqueda |
| Perception | sensores con intervalo propio (visión 3, oído 2, bloques 20, clima 100…), tiers por distancia (×1/×2/×4, ÷2 en alerta), presupuesto de rayos y de NPC, memoria acotada | 300 rayos/tick, 96 NPC/tick |
| Scheduler | 6 buckets por distancia (10…600 ticks), escalonado por fase, cuota por tick, presupuesto de tiempo, escala de población, energía integrada por tiempo | 16 evaluaciones/tick, 1500 µs |

## Medido

**Scheduler** (`SchedulerPerformanceTest`, simulación sin Minecraft, tras calentar la JIT; 1000 NPC de 5 tipos, 2400 ticks = 2 min de servidor, la mayoría lejos de jugadores):

| Métrica | Valor |
| --- | --- |
| Coste medio del tick | **≈ 0.69 ms** (máx. ≈ 4.5 ms) |
| Coste medio de una evaluación | **≈ 53 µs** |
| Evaluaciones | 23 781 (cuota máxima posible 38 400) |
| Aplazadas por cuota | 1 625 |
| Escala de población | 3.0 |
| 100 NPC / 1500 NPC | ≈ 53 µs / ≈ 0.79 ms por tick |

Como el coste por tick está acotado por la cuota (16 evaluaciones de ≈ 53 µs ≈ 0.85 ms) y no por la población, el escalado es sublineal.

**Navigation y Perception:** las pruebas unitarias comprueban los presupuestos (`manyWalkersShareTheSearchBudgetWithoutExceedingIt`, `manyNpcsAreObservedWithinTheSharedRayBudget`, y un caminante lejano que debe recorrer el trayecto a la misma velocidad con la mitad de decisiones). Las cifras absolutas de cada uno se consultan en vivo con `/samuraiai nav status` y `/samuraiai perception status`.

## Límites de la medición (honestidad)

- Las cifras del scheduler son de simulación; en servidor real hay además el coste de leer NPC/percepción/emociones por evaluación (adaptador), que no se ha medido con cientos de NPC reales.
- No se ha hecho una prueba de carga con cientos de NPC físicos (CustomNPCs) en servidor.
- Las pruebas físicas usan pocos NPC por test.

## Cómo diagnosticar

`/samuraiai nav status`, `/samuraiai perception status`, `/samuraiai scheduler status` (µs medios/máximos, aplazados, fallos). Trazas: `-PnavTrace`.
