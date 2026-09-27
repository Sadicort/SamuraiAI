# Fase 1.7 — Auditoría inicial

Fecha: 2026-09-18. Registro histórico de la auditoría inicial.

## Actualización tras autorización e implementación

El usuario autorizó reparar los fallos dentro de Fase 1.7, manteniendo bloqueadas
la certificación y Fase 2. Se corrigió el arranque dedicado de CustomNPCs; se
repararon instalación, bootstrap y dependencias de Whisper y se verificó
transcripción real en cliente Forge. Se añadió el núcleo automático de auditoría.

Estado actual: **PARTIAL, implementación permitida; no certificado**. Los
resultados actualizados y los pendientes están en
[CHANGELOG_PHASE1_7.md](CHANGELOG_PHASE1_7.md). El resto de este documento
conserva los hallazgos y límites de la revisión inicial, anteriores a los cambios.

Guía: [documento maestro](../../SamuraiAI_Fase_1_7_FOUNDATION_COMPLETE_UNIFICADO.md).
Se leyeron sus 5.696 líneas completas. Este informe no certifica Foundation
Complete ni sustituye el motor automático de auditoría solicitado.

## Alcance y método

Se inventariaron y examinaron mecánicamente los 182 archivos Java de producción,
los 10 archivos Java de pruebas y los 3 recursos de `src/main/resources`.
Se revisaron directamente las rutas críticas de instalación, descarga,
bootstrap, actualización y configuración de voz, además de la evidencia previa
de arranque con CustomNPCs. La inspección mecánica de todos los archivos no
equivale a una revisión semántica completa de todas las clases.

Resultados del análisis estático inicial:

- Sin duplicados detectados por la combinación paquete/nombre de archivo Java.
- Sin imports directos de cliente detectados fuera de `client`.
- Sin imports directos `noppes.*` detectados fuera de integración.
- Creación directa de executors en `AIRequestQueue`, `VoiceRecorder` y
  `EmbeddedWhisperEngine`: pendiente centralización conforme a la guía.
- Sin paquete Foundation implementado: trabajo pendiente, no prueba de fallo
  del arranque actual por sí mismo.

Estos controles no demuestran ausencia de carga transitiva incorrecta,
condiciones de carrera, fugas de recursos o duplicados dentro del JAR final.
`git status` no está disponible: el directorio no es un repositorio Git.
No se han borrado ni sustituido fuentes, dependencias o archivos del usuario.

## Hallazgos verificables

| ID | Gravedad inicial | Evidencia | Efecto |
|---|---|---|---|
| F17-001 | CRITICAL | Log de servidor con CustomNPCs: carga de `net.minecraft.client.Minecraft` en `DEDICATED_SERVER`, seguida de `dir == null` | El perfil con CustomNPCs falla antes de ejecutar GameTests |
| F17-002 | ERROR | `VoiceUpdateService.checkForUpdates()` devuelve siempre `completedFuture(false)` | La actualización de modelos sigue siendo un hook, no una implementación |
| F17-003 | ERROR | `VoiceDownloadManager.download()` mueve `.part` al destino; `VoiceInstallationService.ensure()` verifica después | El modelo se publica antes de validarse; falta una instalación transaccional y rollback |
| F17-004 | ERROR | `ensure()` verifica sincrónicamente y devuelve un future completado si el modelo existe; `bootstrap()` utiliza `whenComplete` y carga el motor | El hash y la carga nativa pueden ejecutarse en el hilo del cliente |
| F17-005 | ERROR | `VoiceSettingsScreen.init()` añade Diagnóstico y Volver | La pantalla no implementa las opciones completas exigidas |
| F17-006 | WARNING | Prueba Whisper opcional de inicialización; smoke cliente limitado al título | No hay evidencia suficiente de dictado real, inserción, micrófono y funcionamiento offline extremo a extremo |

F17-001 procede de una ejecución anterior cuya evidencia se volvió a leer en
esta revisión; **no se ejecutó de nuevo Gradle en este turno**. Comando de esa
ejecución, resultado y contexto: [revisión previa](../phase2/PRECONDITIONS_REVIEW.md).

Evidencia disponible:

- `build/phase1/with-customnpcs/logs/latest.log`, líneas 70–80.
- `build/phase1/with-customnpcs/crash-reports/crash-2026-09-18_00.42.20-server.txt`.
- Dependencia: `libs/CustomNPCs-1.19.2-GBPort-Unofficial-20250701.jar`.
- SHA256 de esa dependencia:
  `C6BC3E64BDD604B1133F25AAECC52782693FFF8CC70D23D50A224D515FDA7076`.

La excepción se origina en el arranque de CustomNPCs. Un adaptador opcional de
SamuraiAI no corrige por sí solo código de inicio defectuoso de otro mod.
Las alternativas por investigar son una versión compatible o una corrección
acotada a esa versión, conservando el artefacto original y probando ambos lados.
Los logs bajo `build` son temporales; un `clean` puede eliminarlos.

## Regla que detiene la implementación

La regla 2 de la guía, líneas 232–234, establece:

> Si falla cualquier verificación crítica, detener inmediatamente la implementación.
>
> Nunca continuar.

Con F17-001 confirmado en la evidencia disponible, no se modificó código de
producción ni se implementaron nuevos sistemas. Hace falta aclarar si esta
regla bloquea el avance/certificación, pero permite las reparaciones dentro de
la propia Fase 1.7. La propuesta está en
[decisiones y ampliaciones](GUIDE_DECISIONS_AND_ADDITIONS.md).

## Verificación y riesgos pendientes

No se acredita Minecraft iniciado, Whisper READY, transcripción correcta,
compatibilidad aprobada ni resultados de rendimiento nuevos. No se capturó audio.
Se debe repetir la matriz de pruebas después de cada reparación; resultados
antiguos no certifican un artefacto nuevo.

Orden propuesto una vez resuelta la regla:

1. Completar la auditoría semántica y una matriz requisito → clase → prueba → evidencia.
2. Corregir y probar el arranque dedicado con/sin CustomNPCs y el cliente.
3. Reparar instalación/carga de voz, cancelación y propiedad de recursos;
   probar corrupción, interrupción, rollback y arranque offline.
4. Completar Foundation, aislamiento, runtime, eventos, colas, configuración y
   diagnóstico reutilizando las interfaces actuales.
5. Ejecutar regresiones, pruebas con hardware, benchmarks y certificación del
   JAR exacto que se vaya a distribuir. Mantener bloqueada la Fase 2 hasta cumplir
   sus requisitos.

## Cambios de esta revisión

Solo documentación y un estado JSON informativo. Ninguna clase modificada.
Véase [CHANGELOG_PHASE1_7.md](CHANGELOG_PHASE1_7.md).
