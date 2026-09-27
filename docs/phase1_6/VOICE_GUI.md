# GUI y overlay

## Actualización 2026-09-19

`gui/VoiceSettingsScreen` tiene dos páginas de opciones, diagnóstico, reset y
recarga. Las opciones persistentes siguen siendo responsabilidad de
`VoiceForgeConfig`; la pantalla no accede al grabador directamente. Shift + clic
en Mic abre Settings.

Push To Talk usa `VoiceKeyModeController`: START solo en el flanco de pulsación y
STOP solo en el de liberación. Así una sesión en PROCESSING no se cancela y
reinicia mientras V sigue presionada.

Se conserva el botón/overlay de Fase 1.5, ahora respaldado por Voice Core. La
tecla V y el botón sólo arrancan una sesión cuando el sistema está habilitado.
El texto se inserta en el `EditBox` de `ChatScreen`, permanece editable y el
envío automático sigue siendo opcional.

`gui/VoiceSettingsScreen` ofrece el punto de entrada de diagnóstico y retorno;
las opciones persistentes siguen siendo responsabilidad de `VoiceForgeConfig`.
La pantalla no accede al modelo ni al grabador directamente.
