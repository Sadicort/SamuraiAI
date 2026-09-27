# Fase 2 — Revisión de requisitos previos

> Informe histórico de la revisión inicial. Para el estado posterior a las
> reparaciones y el requisito Foundation 1.7 READY de la guía v2.0, consultar
> [FOUNDATION_GATE_V2.md](FOUNDATION_GATE_V2.md). Los fallos descritos aquí no
> deben interpretarse como fallos actuales sin revisar esa actualización.

Fecha: 2026-09-18. Java 17.0.12, Forge 43.5.2, Minecraft 1.19.2, Gradle 8.4, Windows 11.

## Resultado

**No habilitada para implementación.** El documento maestro exige terminar las
fases 1 y 1.6 antes de comenzar el Behavior Engine. La revisión encontró un
bloqueo reproducible de CustomNPCs y requisitos de voz todavía incompletos.
Esta revisión no acredita el cierre de ninguna fase ni implementa behaviors.

Los resúmenes históricos `docs/PHASES_2_5_CHANGELOG.md` y `docs/phase3/README.md`
usan una numeración distinta. Sus afirmaciones no equivalen al cumplimiento
del nuevo documento maestro de Fase 2.

## Prueba ejecutada en esta revisión

```powershell
gradle runGameTestServer -PwithCustomNpcs=true --stacktrace
```

La primera ejecución no pudo iniciar Gradle por restricciones del entorno sobre
`native-platform.dll`. Se repitió fuera del sandbox con aprobación y Gradle
compiló las fuentes e inició Forge. Resultado final: `BUILD FAILED`, código 1.

El refmap de CustomNPCs se remapeó usando `build/createSrgToMcp/output.srg`.
El bloqueo posterior fue:

```text
Attempted to load class net/minecraft/client/Minecraft for invalid dist DEDICATED_SERVER
noppes.npcs.CustomNpcs.getLevelSaveDirectory(CustomNpcs.java:307)
noppes.npcs.controllers.PlayerDataController.<init>(PlayerDataController.java:26)
NullPointerException: Cannot invoke "java.io.File.listFiles()" because "dir" is null
```

Artefacto probado: `libs/CustomNPCs-1.19.2-GBPort-Unofficial-20250701.jar`.

Evidencia generada:

- `build/phase1/with-customnpcs/logs/latest.log`
- `build/phase1/with-customnpcs/logs/debug.log`
- `build/phase1/with-customnpcs/crash-reports/crash-2026-09-18_00.42.20-server.txt`

El fallo ocurre en `ServerAboutToStartEvent`, antes de ejecutar los GameTests.
Por tanto, esta ejecución no verifica spawn, navegación ni controlador físico.
La compilación correcta y los resultados históricos sin CustomNPCs no compensan
este fallo. Los archivos bajo `build` son evidencia temporal que un `clean`
puede eliminar; se conserva aquí el diagnóstico y el comando reproducible.

## Requisitos de Voice Core pendientes, comprobados en código

| Hallazgo | Evidencia | Consecuencia |
|---|---|---|
| Actualización de modelos sin implementación | `VoiceUpdateService.checkForUpdates()` siempre devuelve `false` | No hay comprobación de manifest remoto ni actualización automática |
| Configuración gráfica parcial | `VoiceSettingsScreen.init()` solo añade Diagnóstico y Volver | No permite cambiar idioma, dispositivo, sensibilidad y demás opciones exigidas desde la pantalla |
| Carga local potencialmente síncrona | `VoiceInstallationService.ensure()` devuelve un future ya completado si existe un modelo válido; `VoiceEngineManager.bootstrap()` usa `whenComplete` | La verificación y la carga nativa pueden ejecutarse en el hilo que inicia el bootstrap; `VoiceGuiEvents` lo encola en el cliente |
| Instalación no transaccional | `VoiceDownloadManager` reemplaza el destino antes de verificar el hash; `VoiceInstallationService` verifica después y borra el destino inválido previo | No se cumple preservar la versión anterior hasta validar la nueva ni rollback |
| Prueba de voz insuficiente para aceptación | `WhisperRuntimeTest` es opcional y solo inicializa el contexto del modelo | No demuestra captura, transcripción, inserción en chat ni funcionamiento offline extremo a extremo |
| Smoke cliente limitado | `ClientSmoke` comprueba la pantalla de título y cierra el cliente | No espera READY ni ejercita Whisper |

No se volvió a grabar audio ni a ejecutar el cliente en esta revisión.
El arreglo de empaquetado previo es una mejora concreta, pero no demuestra que
se hayan completado todos los requisitos de Fase 1.6.

## Arquitectura física existente que debe reutilizarse

| Componente | Estado observado | Trabajo necesario para el documento maestro |
|---|---|---|
| `DefaultBrain` | Decide Goal, obtiene Behavior y ejecuta una lista de Tasks | Delegar la ejecución del plan al Behavior Engine, manteniendo la decisión en Brain |
| `behavior.BehaviorRegistry` | Registra Patrol, Flee, Combat, Investigate y Protect | Reutilizar el registro; completar behaviors y evitar un segundo registro divergente |
| `DefaultActionExecutor` | Solo despacha `TalkAction` | Completar el despacho físico y resultados observables |
| `MoveToTask`, `LookAtTask` | Llaman al controlador directamente | Pasar por Actions; comprobar fallos y cancelación física |
| `MoveToTask` | Retorna SUCCESS si el controlador carece de cuerpo | Distinguir comportamiento no soportado de movimiento físicamente completado |
| `PatrolBehavior` | Cuatro destinos alrededor de la posición actual | Ancla estable, rutas configurables, progreso local y reanudación tras interrupción |
| `MoveToTask.cancel()` | Solo modifica contador | Detener la navegación y liberar acciones activas al interrumpir |
| `NPCTickService` | Intervalo y distribución inicial por UUID | Presupuesto por tick y frecuencia física independiente de la decisión |
| `WorldPerceptionSystem` | Jugadores, radio y raycast | FOV, entidades adicionales, estímulos auditivos y atención |
| `CustomNPCsController` | Adaptador aislado con navegación vanilla, mirada y ataque | Mantener `noppes.*` en integración; ampliar acciones y comprobar convivencia con IA de CustomNPCs |
| `GoalType` | IDLE, REST, PATROL, TALK, INVESTIGATE, PROTECT, COMBAT, FLEE | Definir selección de follow, escort, guard, observe, sit, wander y meditation |
| `Phase3Test` | Registro de behaviors y finalización sin cuerpo | No es una prueba de desplazamiento físico; añadir GameTests verificando posiciones y resultados |

No se detectó un `AGENTS.md` en la raíz ni en su directorio padre; las carpetas
`.agents` y `.codex` no estaban presentes. No se delegó trabajo a subagentes.

## Secuencia para desbloquear la implementación

1. Resolver la compatibilidad del artefacto CustomNPCs con servidor dedicado,
   mediante una versión compatible o una corrección explícita de integración.
   Mantener el JAR original para comparación y no sustituir dependencias a ciegas.
2. Repetir arranque y spawn/remove con y sin CustomNPCs; validar también cliente.
3. Completar los requisitos abiertos de voz y validar el JAR distribuido con
   instalación limpia, modelo existente, modo offline y transcripción real.
4. Con ambos requisitos previos cerrados, implementar el nuevo motor sobre las
   interfaces existentes. Añadir primero estados/resultados/cancelación, luego
   patrulla y navegación, después behaviors reactivos, percepción y depuración.
5. Probar los resultados físicos en GameTests: llegar a destinos, detenerse,
   conservar progreso tras interrupción, no cruzar restricciones, atravesar
   puertas/escaleras y reaccionar a percepción. El registro de clases o un log
   no sustituyen estas comprobaciones.

## Cambios de esta revisión

Se añadieron este informe y `PHASE2_CHANGELOG.md`. No se modificaron fuentes de
producción, configuración Gradle ni dependencias. Gradle generó clases, recursos,
configuraciones de prueba, logs y el crash report bajo `build`.

## Riesgos pendientes

Implementar encima de la prueba fallida dejaría sin validar la integración física
que esta fase necesita. Además, ejecutar carga/hash del modelo en el hilo cliente
puede causar pausas aunque el reconocimiento posterior use un worker. Antes de
considerar cerradas las fases previas se deben comprobar ambos escenarios.
