# Depuración del mundo

## Texto (`living/world/debug/WorldInspector.java`)

| Comando (op) | Muestra |
| --- | --- |
| `/samuraiai living world` | resumen: regiones por LOD, asentamientos, caminos, eventos, población |
| `/samuraiai living world region` | la región donde estás (la crea si no existe): tipo, bioma, peligro, depósitos, fauna, vecinos, contadores |
| `/samuraiai living world settlements` | asentamientos, tipo, región, estado |
| `/samuraiai living world roads` | tramos, longitud, peligro, bloqueos |
| `/samuraiai living world events [archive]` | eventos abiertos o archivados |
| `/samuraiai living world event <tipo> <severidad>` | programa un evento en tu región / asentamiento más cercano |
| `/samuraiai living world resolve <evento> si\|no` | resuelve un evento antes de tiempo |

## Partículas (`living/server/LivingDebugRenderer.java`, `/samuraiai living debug`)

Solo las ve el operador que lo activa, cada 20 ticks, en 160 bloques: borde de cada aldea (verde en paz, amarillo en alerta/recuperación, rojo en peligro/ataque), edificios como columnas (blanco construido, gris planificado, naranja dañado, negro destruido/abandonado), camas (azul) y caravanas en camino (punto dorado en la fracción del recorrido hecha).

## Log

`debugLogging = true` en `samuraiai-world.toml` registra cada evento del World Engine en `SamuraiAI/EVENTS`; en `samuraiai-living.toml`, los de todos los motores.
