# Danger Map

Clases: `zones.DangerMap`, `DangerZone`, `ForbiddenZone`, `DangerSource`, `HazardType`, `terrain.HazardAnalyzer`.

## Dos capas

1. **Peligro estático** (`HazardAnalyzer.staticDanger`, 0..`dangerCap`): lava adyacente/debajo (hasta 70), fuego, cactus, y **caídas** (≥3 → +10, ≥6 → +25, vacío → +45) en los cuatro lados.
   Se guarda en `NavNode.staticDanger` y se invalida con el grafo.
2. **Peligro dinámico** (`DangerMap`): zonas esféricas con caída lineal y caducidad (`DangerZone`): explosiones, mobs hostiles, incendios; y cajas prohibidas (`ForbiddenZone`) que son barrera dura.

## Aceptación de riesgo por caminante

`PathPreferences.maxDanger` (un nodo por encima es intransitable) y `dangerWeight` (coste por punto). El coste de una arista es
`base × terreno × material × sesgos + peligro × dangerWeight` (×1,25 de noche). Perfiles: samurái 70/0,15; guardia 75/0,15; mercader 55/0,25; bandido → definir con
`NavigationProfiles.register` (sin código nuevo en el motor).

## Fuentes externas (contrato con Percepción)

`DangerSource.collect(dimension, tick, out)`: cualquier motor aporta peligros sin que navegación conozca sus internos. `NavigationService.registerDangerSource`
las añade a todas las dimensiones; `DangerMap.refresh` las consulta cada 40 ticks. **La Fase 2.3 registrará aquí las amenazas de `ThreatDetectionEngine`.**
Los eventos de mundo ya alimentan explosiones (`NavigationEvents.onExplosion`: zona de radio 7, puntuación 70, 100 ticks).

## Depuración

`/samuraiai nav danger <pos> <radio> <puntuación> <segundos>` crea una zona; el overlay las dibuja en azul.

## Reacción

Una zona nueva sobre una ruta en curso se detecta en `verify` (una celda de un tramo recto con peligro > extremos + 20) y provoca replanificación
(`dangerZoneAppearingOnTheRouteMakesTheWalkerSkirtIt`).
