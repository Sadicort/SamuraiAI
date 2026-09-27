# Depuración de aldeas

## Comandos

| Comando | Permiso | Muestra |
| --- | --- | --- |
| `/samuraiai living village` | todos | la aldea donde estás: población, oficios, edificios, seguridad, eventos, mercado, renombre |
| `/samuraiai living village list` | todos | aldeas con habitantes, seguridad y renombre |
| `/samuraiai living village info <aldea>` | todos | lo mismo para una aldea (nombre con `_` en lugar de espacios) |
| `/samuraiai living village citizens <aldea>` | op | ciudadanos, oficio, horas, casa, estado |
| `/samuraiai living village buildings <aldea>` | op | edificios, tipo, estado, zona |
| `/samuraiai living village visitors <aldea>` | op | visitantes y fase |
| `/samuraiai living village citizen <npc>` | op | ciudadano + plan del día + sesgo de rutina actual con sus razones |
| `/samuraiai living village found "<nombre>" [radio] [plan]` | op | funda una aldea aquí |
| `/samuraiai living village building <tipo> [radio]` | op | registra un edificio aquí |

## Partículas (`/samuraiai living debug`)

Borde de cada aldea coloreado por seguridad (verde paz, amarillo alerta/recuperación, rojo peligro/ataque), columnas por edificio coloreadas por estado, camas en azul, caravanas en dorado (`living/server/LivingDebugRenderer.java`).

`debugLogging = true` en `samuraiai-village.toml` registra los eventos de aldea en el log `SamuraiAI/EVENTS`.
