# Voice Model Manager

`VoiceModelManager` es la única clase que conoce los archivos de modelos. Lee
`assets/samuraiai/voice/metadata/models.json`, selecciona por idioma y resuelve
rutas dentro de `config/samuraiai/voice/models`.

`VoiceInstallationService` descarga a `.part`, mueve atómicamente y valida
tamaño/SHA-256 mediante `VoiceIntegrityChecker`. Una descarga corrupta nunca se
activa; si un modelo existente falla, se elimina antes de reintentar.

El manifest incluye `ggml-tiny-q5_1` multilingüe con URL oficial, tamaño
`32152673` y SHA-256
`818710568da3ca15689e31a743197b520007872ff9576237bda97bd1b469c3d7`.

Para añadir un idioma: publicar el artefacto y su hash, añadir una entrada al
manifest y extender `VoiceLanguageManager`; el resto del sistema no cambia.
