# Surplus Engine

**Código:** `living/economy/scarcity/MarketBalance.java`; `EconomyEngine.balances/planTrade`.

- Excedente cuando la cobertura supera `surplusDays` (20); termina por debajo de `surplusDays × 0,7`. `SurplusStartedEvent` / `SurplusEndedEvent`.
- El excedente **baja el precio** y es lo que los mercaderes cargan en caravanas (`planTrade` usa las existencias por encima de `coverTargetDays`).
- Una gran cosecha (factor agrícola alto) genera excedente de arroz en otoño y, por tanto, caravanas hacia aldeas escasas.
