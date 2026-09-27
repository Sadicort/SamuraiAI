# Changelog — Fase 5.0 Living World

## Añadido

- `living/world`: regiones con identidad (tipo por bioma, nombre determinista, recursos, fauna, vecinos, peligro, contadores), asentamientos (5 tipos), red de caminos con Dijkstra por perfil de viajero y bloqueos por evento, eventos de mundo con ciclo de 6 fases (12 tipos) y eventos espontáneos deterministas, streaming de 5 niveles de detalle, simulación por pasos con puesta al día acotada, fauna logística en forma cerrada, depósitos con regeneración perezosa, registro de población, catálogo de 15 profesiones, motor de interacción con el entorno, 12 eventos de bus, métricas, inspector, 5 secciones persistentes.
- Hub `living/sim`: `LivingWorld` (tick con buckets, día, llegada/partida de NPCs, fundación, sesgo de rutinas, entregas), `LivingBridges` (puertos de los seis motores), `LivingReactions` (reacciones entre motores), `ConditionScanner` (condiciones de estado para misiones), `Outside`.
- Adaptador `living/server`: `LivingService`, `LivingEvents`, `LivingConfig` (7 `.toml`), `LivingCommand` (`/samuraiai living …`), `LivingDebugRenderer`, `BiomeClassifier`, `CognitionOutside`, `ResourceItems`, `ZoneBridge`, `InteractionBridge`.
- Integraciones fuera de `living/`: `RoutineBiasSource` en el Behavior Scheduler, `SchedulerService.rehome`, sección de prompt «TU MUNDO» (`AIContext.world`, `WorldSection`), `NPCInstance.homeAssigned()`, conversaciones → `LivingService.conversation`.

## Pruebas

9 unitarias del World Engine, 4 de integración del hub (`LivingWorldTest`), 3 de arquitectura, 5 GameTests del mundo vivo.

## Corregido durante la integración

- La prueba de rumores de la Fase 3 no limpiaba sus NPCs al tener éxito (la limpieza diferida no corre si la prueba termina antes): los NPCs se guardaban con el mundo de pruebas y rompían la siguiente ejecución.
- Una prueba de economía dependía de una probabilidad de emboscada de 0,95.
