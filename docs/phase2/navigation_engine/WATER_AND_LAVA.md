# Agua y lava

## Lava

Nunca transitable: `analyze` no admite lava en pies/cabeza; sobre lava no hay suelo; `PathCostModel` da coste infinito a `TerrainType.LAVA`. Un destino en o sobre lava se rechaza (`DestinationResult.LAVA` → fallo `DANGER`).
La lava adyacente suma peligro estático (lado 60, debajo 70), por lo que los perfiles cautelosos la evitan por proximidad. Verificado: `lavaIsNeverEnteredAndDestinationOnLavaIsRejected`.

## Agua

- **Poco profunda** (pies en agua, suelo sólido debajo): `WADE`, coste ×3 (`TerrainType.WATER`), ×1,2 más en tormenta. Se permite con `allowWade` (defecto sí).
- **Profunda**: solo con `allowSwim` (defecto no), a nivel constante en superficie (`SWIM`).
- Sin `allowWade` un río ancho es infranqueable y la petición falla o queda parcial (`shallowWaterIsWadedOnlyWhenAllowedAndCostsMore`).

## Preparado / pendiente

Natación vertical y buceo; "esperar a que baje el agua" es la política `WAIT` genérica. Solo personalidades especiales podrán aceptar lava en fases futuras (subir `maxDanger` no basta: sigue siendo infinita a propósito).
