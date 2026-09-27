# Path Cache

Clase: `cache.PathCache`. Una por dimensión, solo hilo del servidor.

- LRU acotada (`cacheCapacity`, 0 la desactiva) con caducidad por tick (`cacheTtlTicks`).
- Clave: `(dimensión, inicio, meta, firma de preferencias)`. La firma (`PathPreferences.cacheSignature`) incluye peligro, caída, puertas, agua, sesgos y radio del cuerpo:
  dos caminantes solo comparten ruta si enrutarían igual.
- **Reutilización de sufijo**: si un caminante está ya sobre una ruta cacheada hacia la misma meta, recibe el tramo restante (`NavigationPath.suffixFrom`). Las patrullas repiten tramos, así que es común.
- Solo se cachean rutas completas (una parcial describe un límite de búsqueda, no el mundo).
- Al recuperar una ruta se **revalida** contra el mundo vivo (`cachedPathStillValid`) antes de usarla.

## Invalidación

`invalidate(pos)` elimina las entradas cuyas rutas cruzan el chunk del bloque (y el vecino si está a ≤1 del borde); `invalidateChunk`; `purgeExpired` cada 100 ticks; `clear` al recargar la configuración.
Índices por chunk y por meta hacen ambas operaciones baratas. Verificado: `repeatedRouteIsServedFromCacheUntilTheWorldChanges`.

## Métricas

`stats()`: tamaño, capacidad, aciertos exactos, aciertos de sufijo, fallos, expulsiones, invalidaciones, caducadas, tasa de acierto. Se muestran en `/samuraiai nav status`.
