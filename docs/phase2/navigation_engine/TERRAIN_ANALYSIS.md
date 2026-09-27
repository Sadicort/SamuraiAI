# Terrain Analysis

Clases: `terrain.TerrainType`, `Material`, `BlockProfile`, `TerrainCostTable`, `TerrainScanner`, `TerrainSnapshot`, `HeightAnalyzer`, `TerrainOverrides`.

## Clasificación (`NodeAnalyzer.classify`)

`SAFE, NORMAL, ROUGH, WATER, LAVA, CLIFF, STAIRS, LADDER, BRIDGE, DOOR, FOREST, VILLAGE, CUSTOM`. Orden de decisión: override de zona
(`VILLAGE`/`CUSTOM`) → puerta → escalera de mano → agua → escalones → puente (vacío a ambos lados opuestos) → precipicio (caída ≥3 en un lado) →
bosque (hojas encima) → rugoso (≥2 lados bloqueados) → seguro (camino con peligro 0) → normal. `LAVA` es solo un multiplicador prohibitivo: un nodo
sobre lava no es transitable.

## Costes configurables (`TerrainCostTable`)

Multiplicador por `TerrainType` y por `Material` del suelo (barro 1,8; arena/nieve 1,3; hojas 1,5; agua 3; precipicio 2,2…). Todo ≥ 1
(mantiene la heurística admisible). Se sobreescribe por configuración: `costOverrides = ["MUD=2.0", "ROUGH=1.8"]`.

## Escáner

`TerrainScanner.scan(centro, radio, tick)` recorre un cuadrado con búsqueda de superficie ±2 en cada columna, **calienta la caché del grafo** y
devuelve un `TerrainSnapshot` (recuento por tipo, celdas transitables, peligro medio/máximo). Se guarda `100` ticks. Comando: `/samuraiai nav terrain <npc>`.

## Altura

`HeightAnalyzer.analyze(path)`: ascenso/descenso total, mayor subida/caída, número de saltos, escalones, escaleras y caídas.

## Clasificación de bloques (Minecraft)

`MinecraftNavWorldView.compute`: puertas y portillos → `DOOR_WOOD`/`DOOR_IRON`; `BlockTags.CLIMBABLE` → escalera; lava/agua sin colisión;
fuego; telaraña, nieve en polvo, arbusto de bayas, cactus, magma y hogueras → `DAMAGING`; escaleras, losas, vallas/muros, hojas, hielo,
barro, arena, nieve y "camino" (camino de tierra, tablones, ladrillos de piedra, adoquín) → materiales propios.
