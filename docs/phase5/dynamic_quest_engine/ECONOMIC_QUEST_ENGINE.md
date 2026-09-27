# Misiones económicas

| Misión | Origen | Resolución |
| --- | --- | --- |
| Hambre en {aldea} | `ScarcityStartedEvent` (comida) | entrega; se fusiona con otra condición del mismo recurso en la misma aldea |
| {Oficio} sin {recurso} | escasez de una entrada de taller | entrega; cadena → «El regalo de {giver}» |
| Escoltar la caravana | caravana perdida / bandidos | la caravana del asentamiento llega con el jugador cerca (64 bloques) |
| Reabrir el camino | ruta comercial cortada | combatir en el punto del tramo; resolver el evento que la corta |

- Las **recompensas en monedas** salen del tesoro del asentamiento (nunca se crean) y las **en objetos** de su almacén (con su procedencia) → objetos reales en el inventario del jugador (`ResourceItems`).
- Si la escasez termina sola (llega una caravana, una buena cosecha), la misión se cierra como `RESOLVED_BY_WORLD`.
- Contratos: la escasez también ofrece un **contrato de entrega** pagado (`../economy_trade_engine/CONTRACT_ENGINE.md`): misión y contrato son dos vías distintas, cada una con su dueño.
