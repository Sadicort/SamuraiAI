# Voice Core Embedded - Arquitectura

Voice Core añade una capa cliente debajo de la GUI existente: bootstrap,
instalación de recursos, manifest de modelos, integridad, estado del motor,
diagnóstico y proveedor `EmbeddedWhisperEngine`. `VoiceManager` sólo consume la
interfaz `SpeechRecognitionService`; el texto sigue entrando por `ChatScreen`.

La capa no importa Brain, Runtime, DialogueRouter ni APIs de servidor. En
servidor dedicado las clases client-only no se registran.

## Runtime

`io.github.ggerganov:whispercpp:1.4.0` se incluye mediante Jar-in-Jar. Aporta
binding Java/JNA y la biblioteca nativa de Windows x64 dentro del artefacto del
mod. El modelo se descarga bajo demanda a `config/samuraiai/voice/models`, se
valida y se carga desde disco; no se instala un ejecutable separado.
