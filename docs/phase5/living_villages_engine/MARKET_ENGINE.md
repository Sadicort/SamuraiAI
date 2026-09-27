# Market Life Engine (aldea)

**Código:** `living/village/market/MarketLifeEngine.java`. Precios y mercancías: `../economy_trade_engine/MARKET_ENGINE.md`.

- El mercado (edificio `MARKET` funcional) **abre** en su horario salvo que un evento lo cierre (ataque) o la aldea esté en `ATTACK`; un mercado especial o un festival lo **alargan** hasta el atardecer y la noche. `MarketOpenedEvent` / `MarketClosedEvent`.
- **Puestos:** cada mercader ciudadano recibe un puesto en círculo alrededor del mercado.
- **Afluencia:** ciudadanos cuyo día los tiene en el mercado + visitantes haciendo negocios + mercaderes; la economía la lee como demanda y la aldea como vida social.

Prueba: `theMarketOpensWithMerchantsAndClosesUnderAttack`.
