# Settings

## Pantalla dentro del juego

`VoiceSettingsScreen` edita y guarda: habilitado, idioma, dispositivo, inserción,
autoenvío, mantener-V, overlay, sensibilidad, ganancia, duración máxima,
subtítulos y debug. También ejecuta diagnóstico, restaura defaults y recarga el
motor. Se abre con Shift + clic en `Mic`. Enumerar dispositivos no abre ninguno.

`VoiceForgeConfig` registra configuración `CLIENT`: habilitado, idioma,
micrófono, sensibilidad, ganancia, duración máxima, inserción/envío automático,
push-to-talk, overlay, subtítulos y debug. Los valores se normalizan en
`VoiceConfig.Values` y se publican como snapshot inmutable.

El servidor nunca necesita leer esta configuración.
