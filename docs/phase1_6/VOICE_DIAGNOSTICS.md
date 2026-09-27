# Diagnóstico

`VoiceDiagnostics.run` informa estado del motor, micrófono, dispositivo, modelo,
detalle y espacio libre. `VoiceDebugLogger` usa la categoría `SamuraiAI/Voice`;
el nivel detallado sólo se activa con debug.

El diagnóstico distingue “archivo existente” de “motor listo”. Un archivo no
verificado no habilita reconocimiento; el estado `ERROR` se conserva para que
la GUI pueda explicar el motivo. Los fallos de micrófono, permisos, manifest,
hash o runtime producen mensajes controlados y no interrumpen Minecraft.
