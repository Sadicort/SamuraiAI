# Fase 2.3 — Perception Engine 2.0 (registro)

## Entregado

- Núcleo puro `ai.perception` (sin Minecraft fuera de `world/`): 13 sensores, visión con cono/zonas/raycast acumulativo, oído con radios/atenuación/dirección/incertidumbre determinista, atención con histéresis, sospecha, interés, amenaza, conciencia (6 estados), predicción, memoria (6 tipos), pipeline de filtros, snapshot, eventos (8), métricas, inspector.
- Adaptador Forge: servicio con presupuestos y tiers, hooks de eventos del mundo, config `samuraiai-perception.toml` (mismo record que los ajustes, vía `RecordConfigBinder`), comandos, overlay.
- Integración: `EnginePerceptionSystem`/`PerceptionSystems`, `WorldContext.snapshot()`, `UtilityDecisionEngine.evidenceModifier`, `InvestigateBehavior` con objetivo de investigación, `PerceptionEmotionBridge`, peligro hacia Navigation, `SenseProfile` desde personalidad.

## Pruebas

Unitarias: 60 (`VisionTest` 13, `HearingTest` 11, `SensorSystemTest` 16, `AwarenessTest` 20). Físicas (7, con y sin CustomNPCs): visión, pared, sonido, daño, puente amenaza→peligro, comandos, sonido lejano→investigar.

## Errores reales encontrados y corregidos

`EnvironmentSensor` pisaba el clima; la novedad se juzgaba tras escribir la memoria; SEARCHING se disparaba por un simple foco perdido; el foco caducaba ignorando la duración del estímulo; la importancia de sonidos lejanos era baja; el puente percepción→peligro usaba un umbral fijo.

## Límites conocidos

Sensor de bloques por proximidad; VoiceSensor sin publicador; olfato sin viento ni rastros persistentes; overlay sin verificación visual; un ruido muy breve puede no oírse si el sensor no escanea en su ventana de 60 ticks.

## Puerta de Foundation

`FOUNDATION_STATUS.json` sigue **PARTIAL / phase2Unlocked=false**; se implementó por instrucción expresa del usuario y **no se falseó el estado**.
