# Optimization Engine

Clases `optimize.{TickBucket, OptimizationEngine, CrowdManager}`.

## Buckets

Por distancia al jugador más cercano, urgencia y sueño:

| Bucket | Cuándo | Intervalo (ticks) |
| --- | --- | --- |
| VISIBLE | ≤ 24 bloques o urgente | 10 |
| NEARBY | ≤ 64 | 20 |
| ZONE_ACTIVE | ≤ 128 o su zona está en alerta | 40 |
| FAR | ≤ 256 | 100 |
| SLEEPING | duerme y no está visible | 200 |
| HIBERNATING | > 320 (o dormido lejísimos) | 600 |

Un NPC en emergencia o con una respuesta en curso pasa al bucket VISIBLE (cada 10 ticks); uno **forzado** (`disturb`: acaba de subir su nivel de amenaza o de oír algo) se evalúa en el tick siguiente, sea cual sea su bucket.

## Multitud (`CrowdManager`, `OptimizationEngine.crowdScale`)

- **Escalonado:** cada NPC nace con una fase determinista dentro de su intervalo.
- **Cuota:** `maxEvaluationsPerTick`=16; entre los vencidos, primero los más atrasados (justicia); el resto se aplaza y cuenta como `deferred`.
- **Escala de población:** por encima de `crowdThreshold`=40 NPC, todos los intervalos salvo VISIBLE se estiran (hasta ×`2·crowdIntervalScale`=3).
- **Presupuesto de tiempo:** `budgetMicros`=1500; al agotarse se detiene la tanda del tick (siempre se evalúa al menos uno).
- Como la energía se integra por tiempo transcurrido, evaluar menos no pierde fatiga ni progreso.

## Cifras medidas (`SchedulerPerformanceTest`, 1000 NPC, 2400 ticks, distribución realista de distancias)

tick medio **≈ 0.69 ms**, máximo ≈ 4.5 ms; evaluación media **≈ 53 µs**; 23 781 evaluaciones (≤ cuota de 38 400); escala de población 3.0; reparto: VISIBLE 50, NEARBY 150, ZONE_ACTIVE 260, SLEEPING 7, HIBERNATING 533. Escalado: 100 NPC ≈ 53 µs/tick, 1500 NPC ≈ 0.79 ms/tick. Ver [PHASE2_PERFORMANCE](../PHASE2_PERFORMANCE.md).

Evento: `SchedulerOptimizationEvent` (cambio de escala o evaluaciones aplazadas, como mucho cada 200 ticks).
