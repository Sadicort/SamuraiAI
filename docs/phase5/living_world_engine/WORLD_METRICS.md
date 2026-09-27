# Métricas del mundo

**Código:** `living/world/metrics/WorldMetrics.java`, `living/sim/LivingMetrics.java`; comando `/samuraiai living metrics`.

- **World:** regiones y asentamientos creados, actualizaciones y transiciones de streaming, pasos de simulación, puestas al día y sus pasos, fallos de simulador, eventos creados/cerrados/rechazados, caminos construidos/bloqueados, recursos extraídos, coste por tick y del trabajo diario.
- **Hub (`LivingMetrics`):** ticks, días procesados, eventos despachados, reacciones y reacciones fallidas o descartadas (profundidad > 24), condiciones de misión informadas, problemas de genealogía tras cargar, tiempo por tick de calendario/mundo/aldeas/economía/misiones, tiempo del trabajo diario, último error.
- **Adaptador:** `LivingService.interactionStats()` (escaneos, puntos, camas, fuegos, cultivos) y el último error del tick (`/samuraiai living status`).

Ver `../PHASE5_PERFORMANCE.md` para los presupuestos y los valores medidos en pruebas.
