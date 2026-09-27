# Living World Engine (Fase 5.0)

**Código:** `living/world/**` (motor puro) + hub `living/sim/**` (integración de los seis motores) + adaptador Forge `living/server/**`.

## Responsabilidad

«Qué existe y qué está pasando en el mundo»: regiones (identidad, peligro, recursos naturales, fauna), asentamientos (que existen y dónde), red de caminos, eventos de mundo, *streaming* (nivel de detalle por región), calendario de simulación, población agregada y catálogo de profesiones. **No** gestiona ciudadanos (Village), inventarios (Economy), misiones (Quest) ni familias (Family): esos motores lo consultan a través del hub.

## Mapa de documentos

| Tema | Documento |
| --- | --- |
| Núcleo del motor y API | `WORLD_ENGINE_CORE.md` |
| Regiones | `REGION_ENGINE.md` |
| Recursos naturales | `RESOURCE_REGION_ENGINE.md` |
| Fauna | `WILDLIFE_ENGINE.md` |
| Caminos | `ROAD_NETWORK_ENGINE.md` |
| Nivel de detalle | `WORLD_STREAMING_ENGINE.md` |
| Simulación sin chunks | `SIMULATION_ENGINE.md` |
| Eventos de mundo | `WORLD_EVENT_ENGINE.md` |
| Profesiones | `PROFESSION_ENGINE.md` |
| Rutinas de NPC | `NPC_SCHEDULE_ENGINE.md`, `DAILY_ACTIVITY_ENGINE.md` |
| Hogar | `HOME_ENGINE.md` |
| Interacción física | `ENVIRONMENT_INTERACTION_ENGINE.md` |
| Aldeas y templos | `LIVING_VILLAGE_ENGINE.md`, `TEMPLE_ENGINE.md` (resumen; detalle en `../living_villages_engine/`) |
| Memoria del mundo | `WORLD_MEMORY_ENGINE.md` |
| Población | `POPULATION_ENGINE.md` |
| Métricas y depuración | `WORLD_METRICS.md`, `DEBUG_WORLD_OVERLAY.md` |

## Cómo vive el mundo

1. **Arranque** (`LivingService.install`): se crea el `LivingWorld` con la semilla del mundo, se carga `data/samuraiai/living`, se reanuda el reloj y, si está configurado, se aplica el tiempo offline.
2. **Cada tick**: el calendario avanza; el World Engine hace *streaming* (cada `streamingIntervalTicks`), avanza los eventos de mundo que toca y simula las regiones cuyo paso toca dentro de un presupuesto; aldeas, economía y misiones reciben cada una un *bucket* de ticks distinto (`buckets = 5` en `samuraiai-living.toml`), así que dos sistemas pesados nunca corren en el mismo tick.
3. **Cada día**: eventos espontáneos del World Engine, simulación diaria de familias, planificación de comercio y escaneo de condiciones para misiones.
4. **NPCs reales**: al activarse, un NPC se convierte en ciudadano de la aldea donde está (o de una nueva si `autoVillages`), recibe familia, profesión, casa y cama; el Behavior Scheduler recibe el horario de la aldea como sesgo; al morir en el mundo, su familia lo registra.
5. **Jugadores**: sus posiciones despiertan regiones (LOD), cumplen objetivos de misión (llegar, meditar, matar, hablar, entregar) y reciben noticias de la aldea en la que están.

## Garantías

- Motores puros (sin Minecraft), comprobado por `LivingArchitectureTest`.
- Nada aparece de la nada: todo recurso sale de un depósito regional, una granja, fauna o un jugador, con procedencia (`Provenance`).
- Ninguna escritura a disco por tick: solo secciones sucias, pocas por llamada, cada `saveIntervalTicks`.
- Las regiones lejanas se simulan en pasos gruesos acotados; nunca tick a tick.

Changelog: `PHASE5_CHANGELOG.md`. Visión global: `../PHASE5_MASTER_ARCHITECTURE.md`.
