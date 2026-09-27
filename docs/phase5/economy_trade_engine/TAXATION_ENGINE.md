# Taxation Engine

**Código:** `living/economy/taxation/TaxPolicy.java`; `EconomyEngine.collectLevies`.

| Impuesto | Valor | Destino |
| --- | --- | --- |
| Mercado | `marketTax` 5 % de cada venta | tesoro del asentamiento |
| Diezmo | `templeTithe` 2 % del valor producido | cuenta del templo |
| Tributo | `levyPerCitizen` 0,05 por ciudadano y día (quien tenga dinero) | tesoro |
| Facción | preparado (0 hasta que una facción posea el territorio) | — |

Lo recaudado se guarda por tipo; `TaxCollectedEvent(mercado, diezmo, tributo)` una vez al día. Todo son transferencias: nadie paga lo que no tiene.
