# Market Engine (economía)

**Código:** `living/economy/markets/MarketRuntime.java`; `EconomyEngine.syncMarkets`, `sellFromPlayer`, `buyForPlayer`; comandos `/samuraiai living trade`.

- Un mercado por asentamiento con mercado (tipo o edificio): puestos (los mercaderes que venden allí, cada uno con sus existencias **finitas**), abierto o cerrado (lo decide la vida de mercado de la aldea: `../living_villages_engine/MARKET_ENGINE.md`), volumen de hoy, ventas recientes y tabla de precios.
- **Mercado especial** (evento de mundo `MARKET`): más afluencia y demanda hasta su fin.

## Jugadores

| Comando | Qué hace |
| --- | --- |
| `/samuraiai living economy` | precios de la aldea donde estás |
| `/samuraiai living trade sell <recurso> <cantidad>` | vendes objetos: entran al almacén con **tu** procedencia; el tesoro paga el 90 % del precio (85–95 % según la confianza de sus mercaderes en ti), solo lo que puede pagar (se venden unidades enteras) |
| `/samuraiai living trade buy <recurso> <cantidad>` | compras del almacén al 110 % del precio (115–105 % según la confianza); nunca más de lo que hay ni de lo que puedes pagar; recibes objetos |
| `/samuraiai living coins` | tus monedas |

Las monedas del jugador son una cuenta `PLAYER` del Wealth Engine; no son objetos de Minecraft. Cada venta publica `TradeCompletedEvent` y queda en el libro.
