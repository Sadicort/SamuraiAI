# Fase 5 — Inventario previo (Parte XIX del documento maestro)

Inspección del proyecto antes de escribir código de la Fase 5 (Living World, Villages, Economy, Quests, Calendar, Family).
Cada requisito se clasifica como **EXISTS**, **PARTIAL**, **MISSING** o **REQUIRES_REFACTOR**, con el código que ya lo cubre.

## 1. Arquitectura base (lo que la Fase 5 debe reutilizar)

| Sistema | Estado | Dónde está | Uso en Fase 5 |
| --- | --- | --- | --- |
| Foundation / Runtime Manager / Lifecycle | EXISTS | `foundation/**`, `runtime/ServerScheduler`, `npc/lifecycle` | El motor vivo arranca y para en `Samuraiai.starting/stopping` como los demás. |
| EventBus | EXISTS | `event/NPCEventBus`, `event/EventSink`, `event/NpcEvent` | Todos los eventos de Fase 5 son records `NpcEvent` publicados por `EventSink`. |
| Brain / Behavior Engine | EXISTS | `brain/**`, `behavior/**`, `goal/**` | No se toca: la Fase 5 influye a través del Behavior Scheduler. |
| Behavior Scheduler | EXISTS | `ai/scheduler/**` | Rutinas, estilos de vida, zonas, grupos. Falta un punto de entrada para sesgos externos (calendario, aldea, profesión) → **REQUIRES_REFACTOR menor**: `RoutineBiasSource`. |
| Navigation Engine | EXISTS | `ai/navigation/**` | Sigue siendo dueño del pathfinding físico; la red de caminos de Fase 5 es un grafo abstracto a escala mundo. |
| Perception Engine | EXISTS | `ai/perception/**` | `ThreatDetectedEvent` alimenta la seguridad de aldea. |
| Living Memory / Relationship / Emotion / Knowledge & Society | EXISTS | `ai/memory`, `ai/relationship`, `ai/emotion`, `ai/knowledge`, hub `ai/cognition` | Fuentes de verdad de recuerdos, confianza/respeto, emociones y hechos aprendidos. La Fase 5 les habla solo a través de `CognitionService` (experiencias, reputación comunitaria, historia comunitaria). |
| Combat Brain / Sword Combat / Advanced Enemy AI / Battlefield Engine | **MISSING** | Solo existen `behavior/CombatBehavior`, `task/AttackTask`, `CombatStartedEvent/CombatEndedEvent` | No existe una fase de combate con "Battlefield Engine". La guerra de Fase 5 se modela como **eventos de mundo** (`ATTACK`, `WAR`, `BANDITS`) que el World Engine posee; un puerto `ConflictSignal` recibe los `CombatStarted/Ended` reales y los eventos de percepción. Cuando exista un Battlefield Engine se conectará a ese puerto. |
| Persistencia | EXISTS | `ai/cognition/storage/VersionedStore` (sobre JSON, checksum, `.bak`, migraciones), `npc/persistence` (NBT `SavedData`) | Se reutiliza `VersionedStore` (se le añade un formato configurable sin cambiar el de cognición). |
| Configuración | EXISTS | `config/RecordConfigBinder` + registros `*Settings` | Cada motor de Fase 5 tiene su registro de ajustes y su `.toml`. |
| Debug | EXISTS | overlays de partículas por motor (`*DebugRenderer`), inspectores, comandos | Se añaden overlays e inspectores del mundo vivo. |
| Métricas | EXISTS | `*Metrics` con `AtomicLong` + `Snapshot` | Mismo patrón. |
| Networking cliente/servidor | PARTIAL | Solo voz (`client/voice`); no hay canal de datos de simulación | La sincronización del calendario al cliente se hace con la barra de acción (servidor → cliente, vanilla). Un HUD propio queda fuera. |
| Tests | EXISTS | JUnit (`src/test`), GameTests (`src/gametest`), `ArchitectureRulesTest` | Mismos dos niveles + reglas de arquitectura para `living/**`. |

## 2. Solapamientos que NO deben duplicarse

| Requisito Fase 5 | Estado | Código existente | Decisión |
| --- | --- | --- | --- |
| Calendario / tiempo oficial | PARTIAL | `ai/scheduler/time/WorldCalendar` (eventos cada N días), `Timeline` (periodos del día a partir del `dayTime` vanilla) | El Calendar Engine es la **fuente oficial** (fecha monótona persistente, nunca retrocede). El `Timeline` del scheduler se queda como está (describe la luz del día, que es física); el calendario vivo alimenta al scheduler por `RoutineBiasSource` (festivales, estación, clima). `WorldCalendar` del scheduler sigue siendo el catálogo de "día de mercado" configurable. |
| Historia mundial / timeline | PARTIAL | `SocietyEngine.worldTimeline()` (historia *recordada* por comunidades), `WorldMemoryEngine` (leyendas) | La **cronología objetiva** (qué pasó y cuándo) la posee el Calendar Engine (`WorldTimeline`). La historia de Knowledge sigue siendo lo que las comunidades *recuerdan*; el hub copia a Knowledge solo los hechos públicos significativos, como testigo. |
| Comunidades / aldeas | PARTIAL | `knowledge/society/Community` (miembros, rango, cultura, conocimiento colectivo, tradiciones, reputación por etiqueta) | El Village Engine posee aldea, ciudadanía, edificios, distritos, horarios y seguridad. Cada aldea enlaza **una** `Community` de Knowledge (conocimiento colectivo, cultura, reputación): no se duplica ni la membresía social ni la reputación. |
| Reputación | EXISTS | `Community.standing` (por sujeto y etiqueta), `relationship/reputation` | Las recompensas de reputación de misiones y la reputación familiar no crean otra reputación personal: la personal es la de Knowledge/Relationship; la familiar es una estructura propia de Family con sus causas. |
| Enseñanza | EXISTS | `knowledge/teaching/TeachingEngine` (calidad de una lección) | Maestro/discípulo usa la calidad de enseñanza existente; Family solo registra *quién enseñó qué y cuándo*. |
| Hogar | PARTIAL | `NPCInstance.getHome()` (posición fija al primer avistamiento), zonas `HOME` del scheduler | El Home Engine de Village registra casa/habitación/cama y **escribe** la cama en `NPCInstance.setHome`, que el scheduler ya lee. No hay segundo sistema de casas. |
| Profesiones / rutinas | PARTIAL | `scheduler/lifestyle/LifestyleCatalog` (samurai, guard, merchant, monk, villager) | La profesión (qué produce, herramientas, lugares) es un catálogo nuevo de datos; la forma del día sigue siendo el estilo de vida del scheduler y la profesión solo la sesga. |
| Zonas / edificios | PARTIAL | `scheduler/zone/ZoneRegistry` (HOME, WORK, MARKET, TEMPLE, TRAINING, GUARD_POST, DINING, PLAZA…) | Los edificios de aldea referencian zonas existentes; crear un edificio crea (o enlaza) su zona. |

## 3. Requisitos por fase

### 5.0 Living World
| Requisito | Estado |
| --- | --- |
| WorldEngine, WorldRuntime, regiones, asentamientos | MISSING |
| Road Network (grafo mundial) | MISSING (Navigation cubre el camino físico) |
| World Streaming / LOD / simulación sin chunks / catch-up acotado | MISSING |
| NPC Schedule Engine integrado con Behavior Scheduler | PARTIAL → sesgos externos (`RoutineBiasSource`) |
| Profesiones, hogar, actividad diaria | PARTIAL (ver §2) |
| Interacción con el entorno (puertas, sentarse, dormir…) | PARTIAL: puertas y dormir ya los resuelven navegación/scheduler; encender fuegos y cultivos es nuevo |
| World Event Engine (ciclo PREPARATION→CONSEQUENCES) | MISSING |
| Fauna (poblaciones por región) | MISSING |
| Recursos regionales (origen de la economía) | MISSING |
| Memoria mundial / población | PARTIAL (leyendas en Knowledge) |

### 5.1 Living Villages — MISSING salvo §2 (Community, zonas, hogar).
### 5.2 Economy & Trade — MISSING por completo (no hay recursos, inventarios, precios ni caravanas).
### 5.3 Dynamic Quest — MISSING por completo.
### 5.4 Calendar & Seasons — PARTIAL (§2): faltan fecha persistente, estaciones, clima propio, temperatura, luna, festivales, aniversarios, timeline.
### 5.5 Family, Lineage & Legacy — MISSING por completo (no hay parentesco, edad ni herencia).

## 4. Orden de implementación decidido

El documento pide 5.0 → 5.5 y también "no implementar sistemas posteriores sobre contratos inestables" y "todas las fechas vienen del Calendar Engine". Como **todos** los motores guardan fechas, el **contrato del reloj** va primero:

1. `living/core` — vocabulario compartido (fecha, procedencia, presupuesto, almacén versionado).
2. 5.4 Calendar (reloj, estaciones, clima, luna, festivales, aniversarios, timeline) — dependencia de todos.
3. 5.0 World → 5.1 Villages → 5.2 Economy → 5.3 Quests → 5.5 Family.
4. Integración cruzada (hub `LivingWorld`), simulación offline/LOD, persistencia, rendimiento, debug, tests, documentación.

Cada motor: implementar → compilar → tests → corregir → integración → documentar → siguiente.

## 5. Reglas de dependencia (se comprueban con un test de arquitectura)

- `living/**` fuera de `living/server` no importa Minecraft ni Forge (núcleos puros, testeables sin servidor).
- Los seis motores no se importan entre sí: hablan por **puertos** (interfaces en el propio motor) que implementa el hub `living/sim`. Solo `living/core` es común.
- Solo `living/server` (adaptador) toca `NPCManager`, `CognitionService`, `SchedulerService`, Forge y el mundo.
