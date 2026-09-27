# Perception Engine 2.0

Paquete `yadi.samuraiai.ai.perception`. Un pase de percepción convierte **lo que el mundo ofrece** en **lo que un NPC concluye**, sin decidir nada ni mover a nadie.

## Flujo de un pase (`engine.PerceptionEngine.process`)

1. **Sensores** vencidos observan (`SensorScheduler` decide cuáles: nada escanea cada tick).
2. Los estímulos crudos entran en el **pipeline** (`StimulusPipeline`): distancia → visibilidad → relación → prioridad → cooldown.
3. **Atención** elige el foco (`AttentionManager`, con histéresis y ventana de recuperación).
4. **Memoria** recuerda y desvanece (`PerceptionMemory`, seis tipos).
5. **Sospecha**, **interés** y **amenaza** se actualizan (`SuspicionMeter`, `InterestEngine`, `ThreatEngine`).
6. **Conciencia** se reevalúa (`AwarenessEngine`, máquina de estados).
7. Sale un `PerceptionSnapshot` inmutable y una lista de eventos (`ai.perception.events`).

El motor es **sin estado**: todo lo individual vive en `PerceptionState` (uno por NPC). El núcleo (`ai.perception.*` salvo `world/`) no importa Minecraft; `ArchitectureRulesTest` lo comprueba.

## Contrato con el resto

- Entrada: `Perceiver` (posición, orientación, emociones, objetivo actual, `SenseProfile`) y `PerceptionWorld` (interfaz que oculta el mundo: entidades, bloques, luz, clima).
- Salida: `PerceptionSnapshot(awareness, attention, focus, targets, recentSounds, threatLevel, threatScore, threats, suspicion, suspicious, interests, environment, map, investigation)`.
- **Perception nunca decide ni mueve.** El Brain lee el snapshot (`WorldContext.snapshot()`); el planificador de comportamiento lo traduce a datos propios (`Perceived`).
- Personalidad → percepción: `SenseProfile(visionRange, hearing, curiosity, suspicionGain, fearfulness, attentionSpan)`. Lo fija el `PersonalityEngine` del scheduler (`PerceptionService.setSenseProfile`).

## Adaptador (`ai.perception.world`)

`PerceptionService` (un pase por NPC cuando toca, con presupuestos de NPC y de raycasts), `MinecraftPerceptionWorld`, `PerceptionEvents` (ganchos Forge: romper/poner bloque, explosión, daño, muerte, proyectil, puertas), `PerceptionConfig` (`samuraiai-perception.toml`), `PerceptionCommand`, `PerceptionDebugRenderer`.

## Frecuencia por distancia (tiers)

Cerca de un jugador (≤ `tierNearDistance`=32) ×1; media (≤ `tierFarDistance`=64) ×`midIntervalMultiplier`=2; lejos ×`farIntervalMultiplier`=4; NPC en alerta: intervalos ÷ `alertIntervalDivisor`=2. Presupuestos: `maxNpcsPerTick`=96, `maxRaycastsPerTick`=300.

## Integración verificada

`EnginePerceptionSystem` sustituye a la percepción antigua cuando `PerceptionService.enabled()`; `UtilityDecisionEngine.evidenceModifier` traduce el snapshot en puntuaciones (amenaza sube FLEE/COMBAT/PROTECT, curiosidad sube INVESTIGATE); `InvestigateBehavior` va al `investigationTarget`; `PerceptionEmotionBridge` convierte amenaza/sospecha/interés en miedo, ansiedad y sorpresa; las amenazas percibidas alimentan al `DangerMap` de Navigation como zonas de peligro.
