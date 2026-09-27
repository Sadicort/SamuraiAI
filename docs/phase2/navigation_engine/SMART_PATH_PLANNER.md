# Smart Path Planner

Clases: `planner.PathPreferences`, `PathCostModel`, `PathRequest`, `DestinationValidator`, `DestinationResult`, `NavigationProfiles`.

## No solo el camino corto

`PathCostModel.edgeCost` = `base(arista) × terreno × material × sesgo(terreno) × sesgo(material) × clima + peligro × pesoPeligro`.
Factores: seguridad (peligro), distancia (base), tiempo/esfuerzo (subidas, saltos y caídas cuestan más), terreno, **personalidad** (sesgos en `PathPreferences`),
**emoción/eventos** (peligro dinámico, de noche ×1,25, lluvia sobre barro/arena ×1,25, tormenta sobre agua ×1,2).

## Personalidad → ruta

`NavigationProfiles`: samurái evita barro (×2,5) y terreno rugoso; guardia evita bosque y caídas >2; mercader prefiere caminos abiertos (bosque ×1,8, rugoso ×1,5) y no arriesga.
Es **dato**, no código: `register("bandit", PathPreferences.defaults().withDanger(0.03, 95))`. `PathPreferences.withBias/withMaterialBias/withDanger/withDoors/withWater/withMaxDrop/withBodyRadius/withMode`.
La Fase 2.4 derivará estas preferencias de la personalidad numérica de cada NPC (rasgos 0–100) sobre estos perfiles base.

Verificado: `personalityBiasChangesRouteChoice` (un NPC que odia el barro toma el carril limpio).

## Validación del destino (`DestinationValidator`)

Antes de gastar una búsqueda: fuera del mundo (`OUT_OF_WORLD`), chunk no cargado (`CHUNK_UNLOADED`), sobre lava (`LAVA`), zona prohibida (`FORBIDDEN`), sin suelo (`VOID`), sin posición
transitable cerca (`BLOCKED`), peligro sobre la tolerancia (`DANGEROUS`). Si el punto no es transitable pero hay uno a ≤2 horizontal / ≤3 vertical, lo **ajusta** (`ADJUSTED`).
Mapeo a fallos de sesión: `CHUNK_UNLOADED`; `DANGEROUS/LAVA/VOID → DANGER`; el resto `DESTINATION_INVALID`.

## Fuera de alcance de esta fase

Selección entre varias rutas candidatas en paralelo (se devuelve la de menor coste según el modelo).
