# Changelog — Fase 1.5

- Añadido módulo cliente `yadi.samuraiai.client.voice`.
- Captura Java Sound mono 16 kHz/16-bit sólo bajo demanda.
- Sesiones y estados visuales IDLE/LISTENING/PROCESSING/SUCCESS/ERROR/CANCELLED.
- Proveedor `SpeechRecognitionService` desacoplado y bridge local por stdin/stdout.
- Idiomas AUTO, ES, EN, JA, KO, ZH, PT, FR, DE, IT y RU.
- Configuración CLIENT completa, tecla V toggle/hold-to-talk y detección de mixer.
- Botón integrado en `ChatScreen`, overlay no bloqueante e inserción editable.
- Resultado siempre vuelve al Client Thread; no se cambió Brain ni DialogueRouter.
- Audio en memoria, sin archivos temporales ni escucha continua.
- Verificación Forge/JUnit: `gradle clean check` pasó con 31 pruebas unitarias y
  GameTestServer sin CustomNPCs.

## Limitación explícita

Java 17/Forge no incluye un motor Speech-to-Text offline. Por eso el código
entrega la captura y el contrato completamente funcionales, pero requiere
incorporar un runtime Whisper Java/native y modelo para obtener transcripción
real. No se ejecutan procesos externos; sin runtime, el error es controlado y
Minecraft continúa.
