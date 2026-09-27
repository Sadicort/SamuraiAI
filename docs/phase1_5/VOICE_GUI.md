# Voice GUI

`VoiceGuiEvents` añade un `VoiceMicrophoneButton` al `ChatScreen` mediante
`ScreenEvent.Init.Post`, reduce el ancho del campo para evitar solapamiento y
limpia la sesión en `ScreenEvent.Closing`. El tamaño se calcula con el ancho y
alto actuales, por lo que funciona en pantalla completa y resoluciones pequeñas.

`VoiceOverlayRenderer` muestra estado, duración implícita y nivel de volumen sin
bloquear la pantalla. Los estados son `IDLE`, `LISTENING`, `PROCESSING`,
`SUCCESS`, `ERROR` y `CANCELLED`; el botón permanece compatible con estilos y
resource packs porque usa widgets Vanilla.

La tecla configurable por defecto es `V`. En modo toggle inicia/detiene; con
`holdToTalk=true` empieza al mantenerla y detiene al soltarla.
