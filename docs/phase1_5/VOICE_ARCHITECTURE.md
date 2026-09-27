# Voice Architecture

El módulo vive en `yadi.samuraiai.client.voice` y no se registra en el flujo de
servidor. `VoiceManager` coordina una `VoiceSession`, `VoiceRecorder` captura
PCM/WAV sólo durante la solicitud y `SpeechRecognitionService` abstrae el motor.
El texto se entrega a `VoiceInputController`, que modifica el `EditBox` privado
de `ChatScreen` por reflexión controlada; al enviar, Minecraft usa exactamente
su chat normal y SamuraiAI recibe el mensaje mediante el flujo existente.

No se toca Brain, Memory, DialogueRouter, Ollama ni el protocolo servidor.
`VoiceForgeConfig` es configuración CLIENT y el sistema se puede deshabilitar
sin afectar el mod dedicado.

Para añadir un proveedor, implementar `SpeechRecognitionService` y seleccionarlo
en `VoiceManager`; no se deben introducir llamadas Minecraft en el proveedor.
