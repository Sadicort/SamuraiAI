# Scarcity Engine

**Código:** `living/economy/scarcity/MarketBalance.java`; `EconomyEngine.balances`; hub `LivingReactions.scarcity`.

- Para cada recurso (la comida se juzga en conjunto como `food`) se calculan los **días de cobertura** = existencias / consumo diario.
- **Histéresis:** la escasez empieza por debajo de `scarceDays` (2) y termina solo por encima de `scarceDays × 1,5`.
- **Consecuencias al empezar:** `ScarcityStartedEvent`; el precio sube (pánico `panicDemand`); se anota en la memoria comercial; se ofrece un **contrato de entrega** (`CONTRACT_ENGINE.md`); los mercaderes de otros asentamientos la ven y forman caravanas; el hub abre una misión — `SCARCITY` («traer X») o, si un taller de la aldea necesita ese recurso como entrada, `WORKSHOP_STARVED`; la economía indica qué oficio falta (`mostNeededProfession`), que la aldea usa al asignar oficios; la falta de comida sube el malestar.
- **Al terminar:** `ScarcityEndedEvent`; el hub resuelve las condiciones de misión («el mundo lo resolvió antes que el jugador»).
