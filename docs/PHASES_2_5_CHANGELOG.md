# Fases posteriores — resumen de implementación

La Fase 2 quedó integrada durante la reparación del núcleo: cola FIFO, lanes de
diálogo, turnos, cancelación, fallbacks, diagnóstico de modelo y métricas.

La Fase 3 añade behaviors y acciones físicas desacopladas, percepción con
coordenadas/raycast y eventos de combate. La Fase 4 añade `SavedData` NBT y
reactivación por snapshots. La Fase 5 consolida perfiles Gradle, logging,
configuración, pruebas y matriz operativa.

La única condición externa abierta es el crash del jar CustomNPCs suministrado,
descrito en [phase3](phase3/README.md), [phase5](phase5/README.md) y la
[verificación de Fase 1](phase1/VERIFICATION.md).
