# Agriculture Calendar

**Código:** `living/calendar/agriculture/AgricultureCalendar.java`, `CropCalendar.java`, `CropStage.java`, `GrowingSeason.java`.

## Datos

`rice;plant=2,3;grow=4,5,6;harvest=7,8;rain=0.35;frost=1.5;storm=1.0` — meses de siembra, crecimiento y cosecha, proporción de días de lluvia que quiere (`rain`) y cuánto le dañan helada y tormenta.

| Cultivo | Siembra | Crece | Cosecha |
| --- | --- | --- | --- |
| arroz | 2–3 | 4–6 | 7–8 |
| trigo | 8–9 | 10–12, 1–3 | 4–5 |
| hierbas | 1 | 2–3 | 4–7 |

Un recurso sin línea de cultivo no es estacional (se produce todo el año).

## Temporada de crecimiento

Cada día, cada celda de clima registrada informa del clima dominante del día anterior, sus minutos de lluvia y la temperatura mínima; cada cultivo que se está sembrando o creciendo en esa celda anota un **día bueno** (el tiempo que quería), **seco**, de **helada** o de **tormenta** (`GrowingSeason`).

## Rendimiento

Al empezar la cosecha se fija el factor de rendimiento de ese año y esa celda:

```
f = 0,85 + 0,6·buenos − 0,8·secos − daño_helada·heladas − daño_tormenta·tormentas   (proporciones de días), acotado a [0,2; 1,6]
```

y se anuncia (`HarvestOutlookEvent`): **GREAT** si `f ≥ greatHarvestYield` (1,25), **BAD** si `f ≤ badHarvestYield` (0,7), NORMAL si no. Una mala cosecha es obra del tiempo de la temporada, no de una tirada al cosechar.

## Uso

- `productionFactor(recurso, celda, mes)` multiplica la producción de las granjas (Economy); `stage(recurso, mes)` dice si está en temporada (`harvestSeason`, `offSeason` en `EconomyPorts.Calendar`).
- Gran cosecha → celebración en las aldeas de la región (`celebrateGreatHarvests`) y entrada en la cronología; mala cosecha → entrada en la cronología y, a través de la escasez económica, misiones y contratos.

## Pruebas

`harvestYieldComesFromTheWeatherOfTheGrowingSeason`.
