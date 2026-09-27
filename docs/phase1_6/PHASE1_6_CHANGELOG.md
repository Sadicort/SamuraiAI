# Changelog - Fase 1.6

## 2026-09-19 — Integración con Foundation

- Voice bootstrap, inferencia y captura delegan trabajo al AsyncEngine global.
- La apertura de Java Sound dejó de ejecutarse en el hilo cliente.
- Se añadió una pantalla de configuración completa en dos páginas y persistencia
  mediante `VoiceForgeConfig.save`.
- Shift + clic en el botón Mic abre Settings; clic normal conserva grabar/parar.
- El modo mantener-V usa flancos de pulsación/soltado y no reinicia grabaciones
  durante cada tick de cliente.
- El cambio de configuración permite recargar el motor sin reiniciar Minecraft.
- Los buffers PCM se sobrescriben después de construir el WAV en memoria.
- JUnit cubre los flancos Push To Talk y la recarga/reemplazo del motor.

La compilación y el smoke Whisper real pasaron. La captura desde un micrófono
humano y la interacción manual de todos los widgets siguen sin certificación.

- Añadido Voice Core: estados, bootstrap cliente, rutas automáticas y diagnóstico.
- Añadido runtime Java/JNA de whisper.cpp mediante Jar-in-Jar.
- Añadido manifest multilingüe y descarga automática del modelo `ggml-tiny-q5_1`.
- Corregido el empaquetado Jar-in-Jar: el artefacto distribuible sin sufijo
  contiene Whisper.cpp, JNA y el runtime nativo de Windows; la salida `*-dev.jar`
  queda separada para evitar instalar accidentalmente un JAR incompleto.
- Validación de tamaño y SHA-256 antes de cargar el modelo.
- Eliminado cualquier proveedor por procesos externos; Voice Core no ejecuta comandos.
- Separadas instalación, audio, reconocimiento, GUI, eventos y configuración.
- Añadida documentación completa en `docs/phase1_6`.

## Modelo inicial

El modelo se descarga bajo demanda desde el repositorio oficial de modelos de
whisper.cpp. Es multilingüe, mide 32 MB y se instala una sola vez en
`config/samuraiai/voice/models`. Después funciona sin Internet.
