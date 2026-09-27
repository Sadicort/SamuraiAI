# Fase 1.6 - Estado de implementación

## Implementado

- Voice Core cliente separado por `core`, `audio`, `recognition`, `gui`, `event`, `config`, `util` y `debug`.
- Bootstrap automático en `FMLClientSetupEvent`, estados del motor y diagnóstico.
- Carpetas automáticas bajo `config/samuraiai/voice`.
- Manifest JSON, selección por idioma, SHA-256, descargas `.part`, movimiento atómico y eliminación de modelos corruptos.
- Runtime Java/JNA de whisper.cpp incluido por Jar-in-Jar.
- Modelo inicial multilingüe `ggml-tiny-q5_1` descargado bajo demanda y validado.
- Artefacto distribuible `samuraiai-1.0-SNAPSHOT.jar` verificado con
  Whisper.cpp, JNA y `win32-x86-64/whisper.dll` incluidos mediante Jar-in-Jar.
- Captura Java Sound mono 16 kHz/16-bit en memoria.
- Botón, overlay, tecla V, push-to-talk e inserción editable en `ChatScreen`.
- Eventos internos, configuración CLIENT y pantalla de diagnóstico.
- Documentación completa en los 12 documentos de esta carpeta.

## Verificación

```text
gradle clean check
BUILD SUCCESSFUL
32 tests unitarios (1 smoke opcional omitido sin modelo temporal)
GameTestServer Forge 43.5.2 / Minecraft 1.19.2: 1 test requerido aprobado
CustomNPCs ausente: arranque correcto
Modelo Whisper temporal: 32152673 bytes, SHA-256 verificado
```

También se verificó que no existen referencias a `ProcessBuilder`,
`CommandSpeechRecognitionService` ni `providerCommand`.

## Instalación del usuario

El usuario sólo coloca `SamuraiAI.jar` en `mods`. En el primer uso del chat, el
cliente crea sus carpetas, descarga el modelo oficial, comprueba su integridad,
carga el runtime incluido y después puede reconocer voz sin Internet.
