# Merchant Engine

**Código:** `living/economy/traders/Merchant.java`; `EconomyEngine.registerMerchant`.

- Un mercader tiene **existencias finitas** (un almacén propio), dinero (cuenta de riqueza), mercado de origen, la caravana con la que viaja, reputación, clientes conocidos y contratos.
- `npc` es el ciudadano que encarna, o nulo para una casa comercial que solo existe en la simulación (la de un pueblo de mercado, dotada con `merchantEndowment` 150 monedas acuñadas en la fundación).
- **Personalidad → margen:** codicioso ×1,35, negociador ×1,2, honorable ×1,12, generoso ×1,05.
- **Registro:** un ciudadano con oficio mercader en una aldea con economía recibe puesto (`CitizenJoinedEvent`/`ProfessionAssignedEvent` en el hub).
- **Confianza:** `EconomyPorts.Relations.trust01` (la confianza de las relaciones cognitivas, 0..1): los precios que los mercaderes de una aldea pagan y piden a un jugador se mueven hasta un 5 % según la confianza media que le tienen (`playerTrust`, 0,5 si no hay mercaderes con cuerpo).
- Un mercader está disponible cuando no está de viaje (`availableAt`).

`/samuraiai living economy merchants` (op).
