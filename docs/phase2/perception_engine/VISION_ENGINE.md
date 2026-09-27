# Vision Engine

Clases: `vision.VisionEngine`, `VisionCone`, `VisionZone`, `Raycaster`, `RayBudget`, `VisualTrack`, `VisualTarget`, `VisibilityState`.

**La detección no es nunca solo por distancia.** Por cada objetivo: zona del campo visual → varios rayos desde los ojos a puntos del cuerpo → confianza que integra distancia, luz, movimiento y postura.

## Campo visual

`VisionCone` (geometría pura): FOV horizontal 110°, vertical 90°, alcance 2–24 bloques (`visionNear`, `visionFar`), periferia 40°. Zonas con sensibilidad y alcance propios: `CENTER` (1.00/1.00), `MAIN` (0.85/0.90), `PERIPHERAL` (0.45/0.50), `REAR` (0.10/0.15), `OUT_OF_VIEW`. Un objetivo a la espalda se ve poco, no nada (`visionRearSensitivity`=0.1).

## Raycast

`Raycaster` (Amanatides–Woo sobre vóxeles) **acumula opacidad** en vez de parar en el primer bloque: cristal, hojas y agua adelgazan el rayo; bloques sólidos y puertas cerradas lo matan. Devuelve la transmitancia 0–1. `raysPerTarget`=3.

## Presupuesto

`RayBudget` limita los rayos por tick (`maxRaycastsPerTick`). Si se agota, el objetivo se aplaza (`deferred`) y conserva su último estado; **nunca hay raycasts por tick para todo el mundo**.

## Estados

`VisibilityState`: `VISIBLE`, `PARTIAL`, `OBSTRUCTED`, `LOST`, `MEMORY_ONLY`. Confianza visible ≥ 0.6 (`visibleConfidence`), mínima 0.2; un objetivo se da por perdido a los 40 ticks sin verlo (`lostAfterTicks`). Al cambiar de estado salen `VisionDetectedEvent` / `VisionLostEvent`.

## Pruebas

`VisionTest` (13): zonas, rayos con cristal/hojas/puerta, confianza, pérdida y recuperación, presupuesto. Físico: `seesAHostileAndTreatsItAsAThreat` y `aRealWallHidesTheHostile` (una pared de piedra real oculta al creeper).
