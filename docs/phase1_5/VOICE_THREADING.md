# Voice Threading y privacidad

Java Sound y reconocimiento se ejecutan en workers daemon (`VoiceCapture` y
`VoiceRecognition`). Render Thread y Client Thread nunca esperan al proveedor.
Cuando llega el resultado, `VoiceManager` usa `Minecraft.getInstance().execute`
para modificar el `EditBox` y cualquier envío posterior.

La captura abre `TargetDataLine` al iniciar y la cierra al detener, cerrar chat,
cancelar o alcanzar el límite. El WAV sólo vive en memoria; se escribe al stdin
del proveedor y se descarta al terminar. Nunca hay escucha permanente, subida
automática ni archivo temporal.

Un futuro proveedor debe preservar esta frontera y devolver
`CompletableFuture<SpeechRecognitionService.Result>`.
