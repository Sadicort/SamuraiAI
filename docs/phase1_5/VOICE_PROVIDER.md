# Voice Provider

La interfaz `SpeechRecognitionService` permite Whisper local, whisper.cpp, Vosk,
Windows Speech u otros motores sin cambiar GUI ni ChatScreen. Fase 1.6 elimina
el bridge de procesos externos: `EmbeddedWhisperEngine` es la única selección
del `VoiceEngineManager`.

El proyecto todavía no contiene un runtime de inferencia Whisper ni un modelo
neural. Java 17 y Forge no proporcionan ninguno; por eso el engine devuelve un
error seguro hasta incorporar una implementación Java/native embebida y un
manifest con hashes reales. No se ejecutan comandos ni se envía audio fuera del
cliente.

El resultado contiene éxito, texto, error y latencia. El audio jamás se entrega
al servidor; sólo el texto editado por el jugador viaja mediante el chat normal.
