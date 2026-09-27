# Hearing Engine

Clases: `hearing.HearingEngine`, `SoundEvent`, `SoundLog`, `SoundCategory`, `SoundDirection`, `HeardSound`; sensor `sensors.HearingSensor`.

## Qué se oye

Cada `SoundCategory` tiene **su propio radio** (configurable) escalado por el volumen: pasos andando 6 y corriendo 12, puertas 10, romper bloque 14, poner bloque 8, explosión 64, proyectil 16, daño 18, animales 12, voz 16. Los sonidos se registran en un `SoundLog` por dimensión que conserva 60 ticks (`soundLogTicks`).

## Cómo se oye

- **Las paredes atenúan, no silencian** (`wallDampening`=0.25, medido con el mismo raycast acumulativo).
- **Dirección aproximada**: `SoundDirection` (FRONT … BACK_LEFT, ABOVE, BELOW) relativa a hacia dónde mira el NPC.
- **Localización imprecisa**: la posición estimada tiene un error que crece con la distancia; es **determinista** (mismo sonido y mismo oyente → mismo error), para poder probarlo.
- Umbral de audición 0.05 × sensibilidad 1.0.

## Salida

`HeardSound(sound, direction, intensity, estimatedX/Y/Z, uncertainty)` → estímulo AUDIO → memoria AUDITIVA → sospecha (sonido sin causa) → `investigationTarget` (`PerceptionEngine.investigation()`: persiste mientras la fuerza ≥ 0.12 y se resuelve al llegar). Evento: `SoundHeardEvent`.

## Fuentes

`PerceptionService.sound(...)` desde los ganchos Forge (`PerceptionEvents`): romper/poner bloque, explosión, daño/muerte, proyectil, puertas/vallas/trampillas; pasos de entidades próximas (`HearingEngine.footsteps`). `VoiceSensor` es un canal listo para voz; hoy no tiene publicador (límite conocido).

## Límite de escaneo

Un NPC solo oye un sonido si su sensor escanea mientras el sonido está en el registro (60 ticks). Con NPC lejos de jugadores (tier ×4, intervalo de audición 2) puede perder un ruido breve; las pruebas físicas repiten el ruido hasta que se oye.

## Pruebas

`HearingTest` (11) y físicas: `aBlockBrokenNearbyIsHeardAndInvestigated`, `aFarSoundMakesTheSamuraiInvestigate` (con CustomNPCs el samurái se acerca ≥ 6 bloques al ruido).
