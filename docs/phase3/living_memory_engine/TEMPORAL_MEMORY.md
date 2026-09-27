# Memoria temporal

- `Stamp(gameTime, realMillis, weather)`: el tiempo del mundo es persistente (`level.getGameTime()`), no el tick de sesión.
- Índice por **día** (`MemoryIndex.day`).
- `temporal/RelativeTime`: "hace un momento", "hoy", "ayer", "hace tres días", "hace más o menos una semana", "hace N semanas", "hace mucho tiempo". Preparado para el diálogo.
- El olvido y la evolución usan `lastDecay` persistido: el tiempo transcurrido con el servidor apagado **sí cuenta** al recargar.
