# Fase 1.7 — Registro de cambios

## 2026-09-19 — Foundation modular, async y certificación fail-closed

### Implementación

- Module Loader por metadata con dependencias requeridas/opcionales, prioridad,
  lados, estados, detección de ciclos y rollback inverso.
- Resource Manager global con rutas confinadas, directorios administrados,
  SHA-256, instalación atómica y limpieza acotada de temporales.
- AsyncEngine global con admisión limitada, prioridades, deadlines, timeout,
  interrupción, cancelación por propietario y métricas.
- Voice y deadlines de AIRequestQueue migrados al AsyncEngine; apertura de
  micrófono movida fuera del hilo cliente y buffers PCM borrados tras uso.
- ThreadGuard cliente/servidor/worker y cancelación de propietario al eliminar NPC.
- Lifecycle ampliado con estados oficiales, historial, métricas y evento tipado.
- EventBus con prioridad, orden estable, suscripciones cerrables y métricas.
- BenchmarkEngine con warmups, p50/p95/máximo y umbrales.
- FoundationValidator ligado al SHA-256 exacto del JAR. Solo las 12 categorías
  PASS del mismo artefacto pueden producir READY/`phase2Unlocked=true`.
- Auditores nuevos para lados, módulos, threads y capacidad. En perfiles Forge,
  diez etapas se evaluaron y `validation.certification` permanece NOT_RUN.
- Voice Settings completado en dos páginas y persistido con Forge; apertura del
  micrófono en worker, recarga en caliente y Push To Talk corregido por flancos.
- `TickBudget` limita por cantidad y tiempo el trabajo del Brain por tick y
  difiere de forma segura los NPC restantes al siguiente ciclo.
- `ThreadReportWriter` genera automáticamente `THREAD_STATUS.json` y
  `THREAD_AUDIT.md`, incluyendo deadlocks, tareas activas y métricas async.
- Pruebas de estrés deterministas añadidas: 1.000 solicitudes FIFO sin pérdidas
  y ciclo registro/índices/eliminación de 500 runtimes lógicos sin residuos.
- Empaquetado reproducible habilitado para que el certificado ligado al SHA-256
  no cambie por orden de entradas o timestamps de archivos.

### Pruebas ejecutadas

| Ejecución | Resultado |
|---|---|
| `test` | PASS: 85 casos registrados, 84 aprobados, 1 omitido (prueba nativa opcional) |
| `check` | PASS: dedicado sin CustomNPCs, GameTest y JAR reofuscado |
| `check -PwithCustomNpcs=true` | PASS: dedicado con backend físico, GameTest y JAR |
| `runClient -PsmokeClient -PvoiceSmokeFixture=...` | PASS: Voice READY y transcripción Whisper real en 810 ms |
| Estrés de cola | PASS: 1.000 solicitudes FIFO, 8 activas/992 en espera, orden íntegro y sin pérdidas |
| Estrés de runtime | PASS: 500 runtimes lógicos registrados, indexados y eliminados sin referencias residuales |
| Reproducibilidad | PASS: dos builds limpios consecutivos produjeron el mismo SHA-256 |

Artefacto comprobado: `build/libs/samuraiai-1.0-SNAPSHOT.jar`, 711.353 bytes,
SHA-256 `23FE71845C2BB68635AB877CD775454B8CF358D12EBAA1B0C76BD31398EBEBD8`.
El hash identifica esta compilación local y cambiará al recompilar.

### Estado

Foundation continúa **PARTIAL**. Ya no están pendientes Module Loader ni
AsyncEngine, pero no se emite READY sin pruebas de micrófono/GUI, JAR-only,
rendimiento físico real, matrices exhaustivas y raíz de confianza para updates.
Los errores de recursos del JAR de CustomNPCs suministrado siguen siendo externos.

Documentación: [Module Loader](MODULE_LOADER.md), [Resource Manager](RESOURCE_MANAGER.md),
[Async Engine](ASYNC_ENGINE.md), [Lifecycle/EventBus](LIFECYCLE_EVENTBUS.md) y
[certificación](certification/FOUNDATION_VALIDATOR.md).

## 2026-09-18 — Reparaciones autorizadas y Foundation inicial

El usuario confirmó que se pueden reparar fallos críticos dentro de esta fase.
La certificación y Fase 2 permanecen bloqueadas. No se modificó la guía original.

### Implementación

- Corrección opcional y acotada al CustomNPCs instalado para su directorio de
  guardado en servidor dedicado, sin modificar el JAR de terceros.
- Instalación de modelo transaccional: tamaño y SHA256 antes de reemplazar;
  conservación de versión previa ante fallo; descarga limitada y con timeout.
- Bootstrap Whisper fuera del hilo cliente, single-flight, cancelación y
  descarte de resultados tardíos; errores detallados y cierre seguro de contexto.
- Corrección de classloader con `minecraftLibrary`, eliminación de JNA transitiva
  duplicada y reofuscación del JAR con bibliotecas embebidas.
- Resolución del campo de chat por nombre SRG, respeto de inserción automática,
  comprobación de pantalla activa y cierre de recursos al salir.
- `WhisperNativeContext` evita imprimir dictados en consola. Solo se registran
  estado/errores/latencias, no el contenido transcrito.
- `verifyDistributionJar` integrado en `check`: verifica biblioteca/DLL,
  metadata, mixins, SRG y ausencia de stubs/tests/audio/JNA duplicada.
- Foundation Audit Engine con discovery por servicios, registro, pipeline,
  contexto inmutable, severidades, cobertura pendiente y reportes JSON/Markdown.
- Foundation conectado al common setup y decisiones de arranque del núcleo,
  voz e integración; configuración cliente separada en `ClientBootstrap`.

Detalles por sistema: [CustomNPCs](CUSTOMNPC_COMPATIBILITY.md),
[voz](VOICE_INSTALLATION_REPAIR.md), [dependencias](DEPENDENCY_AUDIT.md),
[auditor](AUDIT_ENGINE.md), [arquitectura](FOUNDATION_ARCHITECTURE.md).

### Pruebas realizadas y resultados

| Ejecución | Resultado y alcance |
|---|---|
| `runGameTestServer -PwithCustomNpcs=true` | PASS: arranque dedicado y prueba existente de spawn/remove de tres tipos; no equivale a toda la compatibilidad |
| `test` tras reparar voz | PASS: 14 pruebas nuevas de instalación/bootstrap; prueba nativa opcional omitida sin ruta |
| Primer `runClient` con fixture | FAIL: modelo instalado/verificado, binding Whisper no visible; diagnóstico que motivó corregir `minecraftLibrary` |
| Cliente con JNA transitiva duplicada | FAIL: classloader/JNA; corregido reutilizando JNA de Minecraft |
| Cliente sin CustomNPCs, tras arreglo | PASS: READY y transcripción real del fixture en 787 ms |
| Cliente con CustomNPCs y contexto privado | PASS: READY y transcripción en 839 ms, sin imprimir el texto |
| `check -PwithCustomNpcs=true`, antes de Foundation | PASS: dedicado con cuerpos físicos y gate estructural del JAR |
| `check`, con Foundation integrado | PASS: 11 pruebas Foundation, suite previa, dedicado sin CustomNPCs y gate del JAR; auditor startup PARTIAL, runtime permitido |
| `check runClient -PwithCustomNpcs=true -PsmokeClient -PvoiceSmokeFixture=...`, con Foundation | PASS: dedicado con CustomNPCs, gate JAR y cliente; READY/transcripción real en 937 ms; Foundation PARTIAL en ambos perfiles |

También hubo un intento de smoke rechazado por PowerShell/Gradle al separar el
argumento `.wav`; se corrigió entrecomillando el parámetro. Un primer compile
de `FoundationAuditTest` falló por una declaración `throws` ausente; se corrigió
y se repitió `check` con éxito. Estos intentos fallidos no se cuentan como PASS.

Suite tras Foundation: 57 casos JUnit registrados, 56 aprobados y 1 omitido
(`WhisperRuntimeTest` opcional). La transcripción nativa se verificó adicionalmente
en los smoke tests del cliente. `check` también ejecuta 1 GameTest que recorre
tres tipos de NPC. No son 150 verificaciones ni certificación completa.

### Artefactos y evidencias

- Modelo oficial descargado automáticamente por el mod en
  `run/config/samuraiai/voice/models/ggml-tiny-q5_1.bin`: 32.152.673 bytes,
  SHA256 `818710568DA3CA15689E31A743197B520007872FF9576237BDA97BD1B469C3D7`.
- Fixture público de prueba en `build/phase1_7/fixtures/jfk.wav`; no se grabó audio.
- Resultados JUnit en `build/test-results/test`; logs dedicados en
  `build/phase1/{with-customnpcs,without-customnpcs}/logs`.
- Informes automáticos en `config/samuraiai/foundation` de cada perfil.
- JAR de pruebas reofuscado en `build/libs/samuraiai-1.0-SNAPSHOT.jar`.
  No se publicó ni se instaló en una carpeta externa del usuario.

No se eliminaron datos del usuario. Solo se limpian temporales propios de las
transacciones de prueba/descarga. Gradle y Minecraft generaron configuraciones,
logs, mundos de GameTest y crash reports en `build`/`run`.

### Pendientes para completar la guía

- Module Loader por metadata, dependencias, estados y rollback.
- Resource Manager global y actualizaciones firmadas de modelos.
- AsyncEngine global, inventario completo de futures y cancelación por propietario.
- Auditoría exhaustiva de lados, hilos, runtime, eventos y lifecycle.
- Cierre de GUI/voz: dispositivo, captura no bloqueante, teclas, configuración,
  pruebas de micrófono y de inserción/envío dentro de un mundo.
- Validación del JAR en runtime de producción, pruebas de stress/compatibilidad,
  benchmarks reproducibles y certificación vinculada al artefacto.
- CustomNPCs todavía informa recursos gráficos ausentes y errores de metadata
  no fatales. No se declara a ese mod libre de errores.

La matriz de fases no se marcó READY ni completa. Se conservó la arquitectura
principal y no se añadieron behaviors, combate ni nuevas funciones de gameplay.

Se reforzó además el GameTest para exigir el backend correspondiente al perfil
seleccionado: un fallback silencioso a chat ya no puede aprobar el perfil con
CustomNPCs. El smoke cliente falla explícitamente si Foundation deshabilita Voice.
La repetición final de `check -PwithCustomNpcs=true`, con esa exigencia de backend,
terminó `BUILD SUCCESSFUL` a las 22:42 AST: GameTest físico y gate del JAR aprobados.

## 2026-09-18 — Lectura completa y auditoría inicial

- Leído el documento maestro completo (5.696 líneas).
- Inventariadas fuentes, pruebas y recursos; comprobados imports directos,
  nombres de clase por archivo y puntos de creación de executors.
- Revisados código de voz y logs previos del fallo dedicado de CustomNPCs.
- Consultada documentación oficial Forge para distinguir lados físicos/lógicos.
- Creado `FOUNDATION_AUDIT.md` con problemas, evidencia, límites de la revisión,
  riesgos y secuencia de verificación propuesta.
- Creado `GUIDE_DECISIONS_AND_ADDITIONS.md` con aclaraciones y mejoras propuestas.
- Creado `FOUNDATION_STATUS.json` como estado informativo manual, no certificado
  ni salida de un FoundationAuditEngine ya implementado.
- Detenida la implementación por la regla 2 del documento maestro, pendiente de
  aclarar si permite reparar los fallos críticos dentro de esta fase.

Clases modificadas: ninguna. Dependencias modificadas: ninguna. No se ejecutaron
de nuevo Gradle, Minecraft, Whisper ni benchmarks en este turno. No se eliminó
ningún archivo. La guía original no se modificó.

Validación documental: comprobar sintaxis JSON, referencias locales, existencia
de evidencia citada y correspondencia entre estado BLOCKED y los informes.
Esta validación no sustituye pruebas funcionales del proyecto.
