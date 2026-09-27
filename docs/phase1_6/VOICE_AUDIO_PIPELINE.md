# Audio pipeline

`VoiceRecorder` abre Java Sound sólo durante una sesión, captura PCM mono
16 kHz/16-bit y genera WAV en memoria. `VoiceSession` mantiene estado, volumen,
duración, texto y error. El diseño deja puntos de extensión para normalización,
reducción de ruido, VAD y buffers sin introducir procesamiento artificial que
pueda degradar la señal.

No se escriben audios a disco y el reconocimiento ocurre fuera del hilo de
renderizado.
