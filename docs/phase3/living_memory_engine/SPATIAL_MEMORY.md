# Memoria espacial y landmarks

`spatial/SpatialMap`: nodos (`SpatialNode`: nombre, `LandmarkKind` HOME/TEMPLE/MARKET/FOREST/BRIDGE/RIVER/CAMP/CAVE/MOUNTAIN/LANDMARK/GUARD_POST/TRAINING/OTHER, posición, visitas, familiaridad, peligro, seguridad, conexiones).

- `visit(...)` fusiona con un nodo de la misma zona/tipo dentro de `nodeMergeRadius` o crea uno nuevo, y **conecta** con el último nodo visitado (≤ `nodeConnectRadius`).
- `impress(place, …)` ajusta peligro/seguridad tras sucesos.
- `near(place, radius)` y `landmarksNear` sirven a Navigation/Perception; **Navigation sigue siendo dueño del cálculo de rutas**.
- Alimentación real: el adaptador registra visitas a zonas del Scheduler y descubre lugares (cueva/río/montaña/bosque) con `PlaceClassifier`.
