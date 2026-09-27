# Bootstrap e instalación

`VoiceBootstrapService` se arranca desde `FMLClientSetupEvent`. Crea bajo
`config/samuraiai/voice` las carpetas `models`, `cache`, `logs` y `temp`, carga
el manifest interno, selecciona idioma, comprueba el modelo y actualiza
`VoiceEngineState`.

Los estados son `UNINITIALIZED`, `BOOTSTRAPPING`, `INSTALLING`,
`DOWNLOADING_MODEL`, `VERIFYING_MODEL`, `LOADING_ENGINE`, `READY`, `BUSY`,
`UPDATING`, `ERROR` y `DISABLED`. Toda excepción queda en `ERROR` sin cerrar
Minecraft.

En una instalación limpia, `ggml-tiny-q5_1.bin` se descarga automáticamente,
se valida con SHA-256 y se carga con el runtime Java/JNA embebido. No se abre
una consola ni se ejecuta un programa externo.
