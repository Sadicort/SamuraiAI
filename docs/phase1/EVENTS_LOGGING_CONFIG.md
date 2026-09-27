# EventBus, logging y configuración

`NPCEventBus` entrega eventos al hilo principal cuando son publicados desde
fuera, aísla excepciones de listeners y registra el fallo sin tumbar el servidor.
La fase incorpora eventos de lifecycle, diálogo, percepción, IA, combate,
relaciones y aproximación del jugador. `RuntimeEvents` conecta daño y logout con
las capas correspondientes.

Los logs de spawn/remove incluyen UUID, nombre, tipo, mundo, estado, timestamp y
thread. `SamuraiLogger` expone categorías dedicadas `CORE`, `BRAIN`, `RUNTIME`,
`LIFECYCLE`, `NPC`, `AI`, `QUEUE`, `DIALOGUE`, `MEMORY`, `EMOTION`,
`RELATIONSHIP`, `EVENTS`, `SCHEDULER`, `CONTROLLER`, `SPAWN`, `CUSTOMNPCS` y
`PERSISTENCE`; cada subsistema puede elevar su nivel de forma independiente.

`SamuraiSettings` mantiene un snapshot inmutable validado (límites de timeout,
cola, cooldowns, prompt, historial y longitud de respuesta). `SamuraiForgeConfig`
aplica cambios de forma atómica y reconfigura la cola sin perder solicitudes en
curso.

Pruebas: `CoreTest.eventListenersAreIsolated` y `OllamaDiagnosticsTest`; revisar
la salida de `gradle check` para configuración autocorregida de Forge.
