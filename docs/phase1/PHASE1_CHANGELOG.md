# Changelog — Fase 1

## Implementado

- Scheduler de servidor con sesiones invalidables y frontera de hilo.
- Cola FIFO acotada de IA con estados, métricas, timeout y cancelación.
- Lanes de diálogo por NPC, turnos, cooldowns y fallbacks deterministas.
- Lifecycle validado, rollback de spawn y eliminación integral de referencias.
- NPCManager con índices UUID/nombre/tipo/estado/mundo y nombres únicos.
- Servicios thread-safe de memoria, relaciones y emociones con eventos.
- EventBus aislado y eventos fundamentales de IA, diálogo, percepción, lifecycle,
  relaciones y combate (sólo infraestructura).
- Configuración validada, diagnóstico de Ollama y hooks de persistencia versionada.
- Adaptador CustomNPCs opcional, dependencia declarada y remapeo de refmap.
- JUnit + Forge GameTests integrados en `gradle check`.
- Análisis completo y visualización de las imágenes del proyecto.
- Retirado `libs/discrimine0-0.1-SNAPSHOT.jar`, artefacto sin referencias; los
  logs generados quedan fuera del control de versiones mediante `.gitignore`.

## Fuera de alcance

Patrulla, movimiento físico nuevo, seguimiento, huida, comercio y combate.

## Riesgo abierto

El jar externo CustomNPCs 1.19.2.20250701 contiene una ruta dedicada que accede
a `net.minecraft.client.Minecraft` y lanza NPE al iniciar. El núcleo sin ese jar
es estable; reemplazar el jar es requisito para declarar compatible el perfil
con CustomNPCs instalado.
