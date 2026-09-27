# Propagación

`PropagationEngine.select`: el narrador cuenta lo que cree con fuerza (rumores sólo si superan `rumorTellThreshold`), lo que el oyente **puede** saber (acceso), y nunca lo que el oyente ya cree igual o ya le contó él. `delay = (base + distancia·delayPerBlock)·f(confianza)·f(relevancia)·(1+miembros·communitySizeDelay)`. La `PropagationQueue` es acotada, sin duplicados, y se entrega con **presupuesto por tick** (`propagationBudget`). Nada viaja globalmente en un tick.
