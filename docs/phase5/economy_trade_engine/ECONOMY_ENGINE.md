# Economy & Trade Engine (Fase 5.2)

**Código:** `living/economy/**` (motor puro), `EconomyEngine`, `EconomySettings` (`samuraiai-economy.toml`), `EconomyPorts`.

## Principio

«Qué se produce, consume, guarda, compra, vende y transporta». **Nada económico aparece de la nada**: cada lote de mercancía tiene origen (un depósito, los animales de una región, una granja, un taller, una caravana, un jugador) y cada moneda entró por un `mint` registrado con procedencia. Los inventarios son finitos: ni mercaderes ni almacenes tienen existencias infinitas.

## Qué posee

Recursos, producción, consumo, almacenes, mercados, precios, mercaderes, caravanas, rutas comerciales, contratos, impuestos, riqueza, escasez y excedente, y memoria comercial. **No** duplica las aldeas: pregunta a través de `EconomyPorts.Villages` quién trabaja en qué, cuántas bocas hay y si el mercado está abierto; ni el mundo: pide depósitos, animales, caminos y peligro por `EconomyPorts.World`.

## Ciclo

- **Paso de asentamiento** (`simulate(asentamiento, desde, hasta)`, desde el simulador de región del mundo: una hora cerca de un jugador, un día lejos): horas de trabajo → mercancías, agua de pozos, consumo, deterioro, impuestos diarios, balances de escasez/excedente (con contratos), precios cada `priceIntervalMinutes`, prosperidad.
- **Caravanas** (`advanceCaravans`, cada `caravanIntervalTicks`, en el bucket de economía del hub) viajan en segundo plano.
- **Comercio** (`planTrade`, una vez al día desde el hub): los mercaderes con excedente en casa forman caravanas hacia donde falta.
- **Mercados** (`syncMarkets`, su propio bucket): puestos y apertura según la vida de mercado de la aldea.

## Documentos

`RESOURCE_ENGINE.md`, `PRODUCTION_ENGINE.md`, `CONSUMPTION_ENGINE.md`, `WAREHOUSE_ENGINE.md`, `MARKET_ENGINE.md`, `PRICE_ENGINE.md`, `MERCHANT_ENGINE.md`, `CARAVAN_ENGINE.md`, `TRADE_ROUTE_ENGINE.md`, `CONTRACT_ENGINE.md`, `WEALTH_ENGINE.md`, `TAXATION_ENGINE.md`, `SCARCITY_ENGINE.md`, `SURPLUS_ENGINE.md`, `ECONOMIC_EVENTS.md`, `TRADE_MEMORY_ENGINE.md`, `ECONOMY_SIMULATION.md`, `DEBUG_ECONOMY_OVERLAY.md`, `PHASE5_2_CHANGELOG.md`.

Pruebas: `EconomyEngineTest` (8), `LivingWorldTest.aCaravanCarriesSurplusBetweenVillages`.
