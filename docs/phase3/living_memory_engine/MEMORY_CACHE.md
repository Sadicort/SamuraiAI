# Caché

`cache/MemoryCache`, niveles:
- **Short**: resultados de recuperación por contexto, con expiración en ticks (`shortCacheTicks`) e invalidación al cambiar los recuerdos.
- **Hot**: los más usados (≥ `hotAccessThreshold`, máx `hotSize`).
- **Warm**: LRU de tocados recientemente (`warmSize`).
- **Cold**: el resto del runtime. **Storage**: disco.
Métricas: tasa de aciertos de la caché corta y de niveles calientes/tibios. Un recuerdo nunca vive sólo en caché.
