# Voice Debug

Con `voice.debug=true` se registra categoría `SamuraiAI/Voice` con dispositivo,
estado, duración, latencia y errores. Los logs nunca incluyen audio ni el WAV.
`VoicePermissionChecker` enumera mixers compatibles y distingue micrófono
ausente, línea ocupada y error del sistema.

Prueba manual:

1. Abrir chat y verificar botón `Mic`.
2. Pulsar V o el botón; comprobar `LISTENING` y barras de volumen.
3. Detener; comprobar `PROCESSING` y que el texto aparezca editable.
4. Enviar y comprobar que el chat normal sigue intacto.
5. Probar chat cerrado, micrófono desconectado, comando vacío y timeout.
