# Depuración de navegación

Todo con permiso de operador (nivel 2).

| Comando | Qué hace |
|---|---|
| `/samuraiai nav status` | resumen: sesiones, recálculos, atascos, tiempos, caché de rutas, caché del grafo, fallos por motivo |
| `/samuraiai nav inspect <npc>` | estado, destino, fallo, último evento, ruta (nodos, actual, coste, longitud, parcial, caché), alturas, posición/terreno/peligro, ETA, recálculos, métricas |
| `/samuraiai nav goto <npc> <x y z>` | inicia un viaje manual |
| `/samuraiai nav cancel <npc>` | cancela |
| `/samuraiai nav terrain <npc>` | escaneo del terreno alrededor |
| `/samuraiai nav danger <x y z> <radio> <puntuación> <segundos>` | crea una zona de peligro |
| `/samuraiai nav debug` | activa/desactiva el overlay para quien ejecuta |
| `/samuraiai nav metrics reset` | pone las métricas a cero |

## Overlay (`NavigationDebugRenderer`)

Partículas de polvo **solo para el operador**, generadas en el servidor (sin mod de cliente ni paquetes propios): verde = ruta, amarillo = nodo actual, rojo = destino,
magenta = celdas bloqueadas, azul = zonas de peligro. Cada 10 ticks, hasta 96 puntos por ruta, alcance 64 bloques. **No se ha verificado visualmente en esta fase** (la lógica de comandos sí en servidor real).

## Trazas

Logger `SamuraiAI/Navigation` (también `SamuraiAI/Performance`). Con `debug.debugLogging = true`: ruta adoptada, ruta inválida (índice, posición, motivo), recálculos (motivo, desde, meta), atascos (posición, índice, acción), puertas, y el estado físico del cuerpo (`MovementBody.diagnostics`: hitbox, velocidad, colisiones libres E/O/S/N).
Así se diagnosticó el hitbox 1×1 de CustomNPCs. En GameTests: `-Dsamuraiai.navTrace=true`.

## Extensión

Un inspector integrado de todo el ecosistema (Fase 2.4) reutilizará `NavigationInspector.inspect`.
