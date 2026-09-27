# Depuración de la economía

**Código:** `living/economy/debug/EconomyInspector.java`; comandos en `LivingCommand`.

| Comando | Permiso | Muestra |
| --- | --- | --- |
| `/samuraiai living economy` | todos | precios de la aldea donde estás |
| `/samuraiai living economy prices <aldea>` | todos | precio, oferta, demanda, factores |
| `/samuraiai living economy settlement <aldea>` | op | tesoro, almacén, producción, consumo, importaciones, exportaciones, déficits, balances, impuestos |
| `/samuraiai living economy provenance <aldea> <recurso>` | op | lotes con su origen |
| `/samuraiai living economy ledger <aldea>` | op | últimos 30 movimientos |
| `/samuraiai living economy merchants` | op | mercaderes, dinero, existencias, estado |
| `/samuraiai living economy caravans` | op | caravanas, carga, progreso, estado |
| `/samuraiai living economy routes` | op | rutas, distancia, peligro, estado, uso |
| `/samuraiai living economy contracts` | op | contratos |
| `/samuraiai living contract list` | todos | contratos abiertos aquí |

Caravanas en camino como punto dorado en `/samuraiai living debug`. `debugLogging = true` en `samuraiai-economy.toml` registra los eventos económicos.
