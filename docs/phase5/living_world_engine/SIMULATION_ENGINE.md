# Simulation Engine — simular sin chunks

**Código:** `living/world/simulation/SimulationEngine.java`, `RegionSimulator.java`.

## Pasos por nivel de detalle

| Nivel | Paso (minutos de Deiliora) | Clave |
| --- | --- | --- |
| FULL / ACTIVE | 60 | `stepMinutesActive` |
| SETTLEMENT | 360 | `stepMinutesSettlement` |
| ABSTRACT | 1440 | `stepMinutesAbstract` |
| HISTORICAL | 10080 | `stepMinutesHistorical` |

Cada `simulationIntervalTicks` (5) se simulan solo las regiones **cuyo paso toca**, las más atrasadas primero, como máximo `maxRegionsPerTick` (4) y dentro de `budgetMicros` (1500 µs, `TickBudget`: la primera unidad siempre corre para que un presupuesto mínimo no bloquee el avance).

## Simuladores registrados (orden)

1. **Fauna** (interno del World Engine).
2. **Aldeas** (hub): `villages.simulate(asentamiento, desde, hasta, fullDetail)` para cada aldea de la región — trabajo planificado, horas de profesión, visitantes, seguridad.
3. **Economía** (hub): `economy.simulate(asentamiento, desde, hasta)` — producción a partir de las horas trabajadas, consumo, deterioro, precios.

Las familias avanzan una vez al día desde el hub (`families.simulate`), no por región.

Un simulador debe escalar sus tasas por `hasta − desde` y **no iterar por tick**: un paso puede ser una hora o una semana.

## Puesta al día acotada

Si una región está muy atrasada (despierta tras meses, o el servidor estuvo ocupado), `catchUp` la lleva al presente en como mucho `maxCatchUpSteps` (48) pasos gruesos: `CatchUpCompletedEvent(región, minutos, pasos)`. Nunca tick a tick.

## Robustez

Un simulador que lanza una excepción se cuenta (`failures`, `lastError`) y no detiene al resto.
