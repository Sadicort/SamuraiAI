# Caravan Engine

**Código:** `living/economy/caravans/Caravan.java`; `EconomyEngine.planTrade/createCaravan/advanceCaravans`.

## Formación (`planTrade`, diaria)

Para cada mercader disponible con al menos 5 monedas, sobre el **excedente** de su almacén de origen (existencias − demanda × `coverTargetDays`, al menos 5 unidades; el agua no se comercia), busca el destino a ≤ `maxTradeDistance` (6000) con precio ≥ 1,15 × el de origen o en escasez y una ruta utilizable, y calcula:

```
cantidad = min(excedente, capacidad de carros / peso × factor de comercio de la estación, monedas / precio_origen)
beneficio = cantidad·precio_destino·merchantSaleShare − cantidad·precio_origen − distancia·transportCostPerBlock·cantidad·peso − peligro·ingresos·0,5
```

Elige el mejor con beneficio ≥ `minProfit` (15). La caravana **compra** la carga al almacén de origen (el tesoro cobra; las mercancías pasan a la carga conservando su origen). Como mucho `maxCaravans` (64) activas.

## Viaje (en segundo plano)

`PLANNED → LOADING → TRAVELLING (→ DELAYED) → ARRIVED → RETURNING → COMPLETED` (o `LOST`, `CANCELLED`). El progreso son bloques a lo largo de los tramos de la ruta: `caravanSpeed` (3) bloques por minuto de Deiliora × el factor de viaje de la estación × el `outdoorFactor` del clima: una caravana lejos de todo jugador cuesta unas operaciones aritméticas; su posición es exacta y se puede dibujar (`/samuraiai living debug`).

- **Tramo bloqueado** → espera (`CaravanDelayedEvent`); más de `maxDelayMinutes` (720) esperando → da media vuelta hacia casa («camino cortado»).
- **Emboscada** al terminar cada tramo: probabilidad `min(0,95, peligro × ambushScale × longitud/1000 × (1 − defensa))`, con `caravanGuards` (2) guardias de defensa; pérdida entre 30 % y 100 % de la carga reducida por la defensa; puede perderse entera (`CaravanLostEvent` → misión de caravana perdida).
- **Llegada:** vende en destino (`CaravanArrivedEvent`), el mercader llega como visitante a la aldea, se cumplen escoltas de misión si hay jugadores cerca; vuelta a casa (`CaravanCompletedEvent` con beneficio).

**No se simulan caravanas físicas lejos de jugadores**; tampoco cerca: no se generan entidades de carro (límite actual).

Pruebas: `caravansCarrySurplusToScarcityAndGoodsKeepTheirOrigin`, `dangerousRoadsLoseCaravansAndBlockedRoadsDelayThem`.
