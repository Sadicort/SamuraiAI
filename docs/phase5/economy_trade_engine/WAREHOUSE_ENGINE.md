# Warehouse Engine

**Código:** `living/economy/storage/Warehouse.java`, `inventory/Inventory.java`, `inventory/ResourceLot.java`.

- **Almacén:** el de la aldea, las existencias de un mercader, la carga de una caravana, la despensa de un hogar. Inventario, capacidad en peso (`warehouseCapacity` 20000; mercader `merchantStockCapacity` 800; caravana `cartCapacity` × carros), propietario, protección 0..1 (contra robo y saqueo), ubicación y contadores de entradas/salidas.
- **Inventario:** lotes por recurso en orden FIFO. `take` devuelve solo lo que hay, partiendo de los lotes más antiguos y conservando sus orígenes. Los perecederos se estropean por día (`SPOILED` en el libro). Lotes acotados: se fusionan los del mismo origen y día y, más allá de `maxLotsPerResource` (12), se pliegan los dos más antiguos.
- **Lote:** cantidad, calidad, durabilidad y **siempre** origen (`Provenance`) y asentamiento donde se hizo. Al dividir o mover, la procedencia sobrevive.
- **Fundación:** un asentamiento nuevo recibe `foundingStoreDays` (5) días de provisiones para `foundingPopulation` (8) personas con procedencia `founding` (visible en la cronología de la economía), para que no empiece con hambre.
- **Pérdidas:** incendio (35 %·severidad), saqueo tras un ataque no defendido (25 %·severidad), crecida (10 %·severidad) → `WarehouseLossEvent`.

`/samuraiai living economy provenance <aldea> <recurso>` muestra de dónde vino lo que hay en el almacén.
