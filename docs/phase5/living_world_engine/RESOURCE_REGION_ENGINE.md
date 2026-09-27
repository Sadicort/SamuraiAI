# Resource Region Engine — el origen de los recursos

**Código:** `living/world/regions/ResourceDeposit.java`, `RegionCatalog.java`; `WorldEngine.extract/available`.

## Regla

**Nada se recolecta que no se saque de un depósito.** La madera sale de los bosques, el mineral de las montañas, la arcilla de los ríos, el agua de los pozos y ríos. La Economy Engine pide a través del hub `extract(región, recurso, cantidad)` y recibe lo que hay; el lote resultante lleva la procedencia `region-deposit`.

## Depósitos integrados (capacidad:regeneración por día)

| Tipo | Depósitos | Peligro base |
| --- | --- | --- |
| MOUNTAINS | piedra 6000:60, hierro 900:4, carbón 1400:7, hierbas 150:6, agua 2000:200 | 0,15 |
| FOREST | madera 4000:45, hierbas 500:15, bambú 900:30, agua 1500:150 | 0,08 |
| VILLAGE | agua 3000:400, madera 400:6, piedra 300:3, arcilla 200:4 | 0,02 |
| TEMPLE | agua 2000:250, hierbas 250:8, madera 300:5 | 0,01 |
| FIELDS | arcilla 700:6, agua 2000:250, hierbas 200:8, madera 300:4 | 0,03 |
| RIVER | agua 10000:2000, arcilla 900:9, bambú 400:15 | 0,04 |
| SWAMP | hierbas 700:22, arcilla 1200:10, bambú 300:10, agua 4000:500 | 0,12 |
| COAST | agua 2000:200, arcilla 300:3, piedra 500:4 | 0,05 |
| RUINS | piedra 1500:0, hierro 150:0 (no se regeneran) | 0,25 |

Líneas configurables (`regions` en `samuraiai-world.toml`): `MOUNTAINS;stone=5000:50;iron=800:4;coal=1200:6;danger=0.15`.

## Regeneración perezosa

`ResourceDeposit.stock(ahora)` regenera hacia la capacidad (`regenPerDay` × días desde la última lectura) **cuando se lee**: una región dormida no cuesta nada. `damage(fracción)` (un incendio forestal, una crecida) reduce el stock; `extracted()` acumula lo sacado.

## Pruebas

`resourcesComeOutOfDepositsAndNeverFromNothing`, `gatheringTakesFromTheRegionAndRecordsProvenance` (economía).
