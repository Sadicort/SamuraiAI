# Fase 1.7 — Instalación y bootstrap de Whisper

## Problemas reparados

Antes se reemplazaba el archivo de modelo y después se verificaba su hash.
Una descarga incorrecta podía perder la versión anterior. Además, comprobar
un modelo existente y cargar su contexto nativo podía ocurrir en el hilo del
cliente. El motor inicial se construía sin modelo y algunos errores posteriores
solo mostraban un mensaje genérico.

## Implementación

- `VoiceDownloadManager`: archivo temporal único junto al destino; HTTP 200;
  tamaño anunciado y bytes recibidos acotados; SHA256 antes de publicar;
  sustitución atómica. Si no existe reemplazo atómico, falla conservando el
  modelo anterior. El temporal de esta operación se elimina al finalizar.
- Solo HTTPS remoto; HTTP de loopback numérico permite pruebas locales. Se
  rechazan credenciales embebidas y fragmentos. Timeout de conexión y plazo
  total de transferencia. No se escribe audio.
- `VoiceInstallationService`: valida el modelo existente y lo reutiliza offline;
  no lo borra antes de descargar un reemplazo.
- `VoiceModelManager`: valida IDs de archivos y mantiene la propiedad de rutas.
- `VoiceThreadDispatcher`: worker de voz con cola acotada y cancelación mediante
  interrupción. Aún no sustituye al AsyncEngine global solicitado por la guía.
- `VoiceEngineManager`: bootstrap asíncrono, una preparación a la vez, errores
  detallados, reintento explícito y cierre que invalida resultados tardíos.
- `EmbeddedWhisperEngine`: un reconocimiento a la vez; libera el contexto nativo
  después de la inferencia, nunca concurrentemente. Limpia arrays de audio al
  terminar. El cierre nativo no bloquea al cliente.
- `WhisperNativeContext`: administra los handles y llama a la API nativa del
  binding sin usar el wrapper que imprimía cada segmento transcrito. Libera
  parámetros con `whisper_free_params` y contexto con `whisper_free`.
- `SpeechRecognitionService`: contrato de disponibilidad y cierre con valores
  predeterminados para mantener proveedores extensibles.
- `VoiceBootstrapService` entrega eventos al hilo cliente; `VoiceManager`
  evita abrir micrófono si el motor no está listo, ignora resultados de pantalla
  obsoleta y respeta la opción de insertar texto automáticamente.
- `VoiceGuiEvents` cierra los recursos al terminar el juego.
- `VoiceInputController` resuelve el campo del chat mediante el nombre SRG
  `f_95573_` y la utilidad de Forge, en lugar del nombre de desarrollo `input`.

No se modificaron Brain, DialogueRouter ni el proveedor Ollama.

## Pruebas

`VoiceInstallationTest`: descarga correcta, hash erróneo, tamaño de cabecera
incorrecto, exceso de bytes chunked, HTTP 404/500, timeout de cuerpo, modelo
instalado offline, preservación del anterior, ID de ruta inválido y HTTP remoto
rechazado. Diez pruebas aprobadas el 2026-09-18.

`VoiceBootstrapTest`: worker fuera del llamador, single-flight, cierre durante
carga nativa simulada, cancelación con reintento y error detallado. Cuatro pruebas aprobadas.
Se ejecutaron junto a la suite existente mediante `gradle test --stacktrace`.
La prueba nativa opcional sin ruta de modelo se omitió, no se contó como aprobada.

Se amplió `ClientSmoke` para esperar READY y transcribir un WAV de referencia
mediante el motor real; no captura el micrófono. Invocación:

```powershell
gradle runClient '-PsmokeClient' '-PvoiceSmokeFixture=build/phase1_7/fixtures/jfk.wav' --stacktrace
```

Fixture público de [whisper.cpp v1.4.0](https://github.com/ggml-org/whisper.cpp/blob/v1.4.0/samples/jfk.wav),
descargado a `build`, no incluido en el mod. SHA256:
`59DFB9A4ACB36FE2A2AFFC14BACBEE2920FF435CB13CC314A08C13F66BA7860E`.
El resultado del smoke se registra en el changelog al finalizar su ejecución.

La prueba real encontró además un problema de classloader y JNA duplicada,
corregido en `build.gradle`; véase [DEPENDENCY_AUDIT.md](DEPENDENCY_AUDIT.md).
Después de corregirlo, Whisper llegó a READY y transcribió el fixture con y sin
CustomNPCs. La repetición con el contexto nativo sin salida de transcripciones
también pasó (839 ms). Es un smoke con WAV conocido, no una prueba del micrófono.

## Límites pendientes

No está implementado aún el actualizador remoto firmado, reanudación HTTP,
progreso de descarga, cambio de modelo en caliente ni rollback ante incompatibilidad
nativa después de validar un archivo nuevo. La conservación transaccional actual
protege frente a fallos de descarga e integridad, no cubre esos escenarios.

La cancelación de una inferencia nativa descarta su resultado pero no preempta
la llamada C; espera que termine para liberar el contexto. Falta certificar el
micrófono real, la GUI completa y el JAR final. La captura actual todavía requiere
revisión de apertura del dispositivo e interrupciones; no se afirma que toda la
Fase 1.7 esté completada.
