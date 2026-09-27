# Eventos económicos

Dominio `economy` (`living/economy/events`):

| Evento | Cuándo |
| --- | --- |
| `ResourceProducedEvent(asentamiento, producido, trabajadores)` | un paso produjo mercancías |
| `ResourceConsumedEvent(consumido, déficit, días de comida)` | un paso consumió |
| `PriceChangedEvent(recurso, antes, después, factor)` | un precio se movió más del umbral |
| `ScarcityStartedEvent`, `ScarcityEndedEvent` | escasez |
| `SurplusStartedEvent`, `SurplusEndedEvent` | excedente |
| `TradeCompletedEvent(vendedor, comprador, recurso, cantidad, precio)` | venta (también a/desde jugadores) |
| `TaxCollectedEvent` | impuestos del día |
| `WarehouseLossEvent(causa, perdido)` | incendio, saqueo, crecida |
| `CaravanCreatedEvent`, `CaravanDepartedEvent`, `CaravanDelayedEvent`, `CaravanAmbushedEvent`, `CaravanLostEvent`, `CaravanArrivedEvent`, `CaravanCompletedEvent` | vida de una caravana |
| `TradeRouteDisruptedEvent`, `TradeRouteRestoredEvent` | rutas |
| `ContractCreatedEvent`, `ContractResolvedEvent` | contratos |
| `EconomicEventEvent(tipo, detalle)` | GREAT_HARVEST, BAD_HARVEST, FIRE, SPECIAL_MARKET, LOST_CARAVAN, BLOCKED_ROUTE, FESTIVAL |

Reacciones del hub: escasez → misión; caravana perdida → misión; emboscada → giro en las misiones de escolta; llegada → visitante mercader y escoltas cumplidas; ruta interrumpida/restablecida → misión abierta/resuelta.
