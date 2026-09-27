# Contract Engine

**Código:** `living/economy/contracts/Contract.java`; `EconomyEngine.offerContract/deliverToContract/resolveContract`; comandos `/samuraiai living contract`.

## Modelo

Tipo (`PURCHASE, SALE, DELIVERY, PROTECTION, TRANSPORT`), emisor (la cuenta del asentamiento), asentamiento, recurso, cantidad, precio unitario, plazo, confianza mínima, motivo, contraparte, misión asociada (la posee el Quest Engine; el contrato solo guarda su id), entregado y estado (`OPEN, ACCEPTED, FULFILLED, FAILED, EXPIRED, CANCELLED`).

## Cuándo se ofrecen

**Por escasez:** cuando un asentamiento entra en escasez de un recurso (para la comida, arroz) y no tiene ya un contrato abierto de ese recurso, ofrece un contrato de **entrega** por lo que le falta para cubrir `coverTargetDays`, a 1,2 × su precio, con plazo `contractDays` (7 días) y como mucho lo que su tesoro podría pagar en ese momento. También se pueden ofrecer por API (misiones, comandos futuros).

## Cumplimiento

- `/samuraiai living contract list` — contratos abiertos de la aldea donde estás.
- `/samuraiai living contract deliver <id> <cantidad>` — entregas objetos en la aldea que lo pidió: entran al almacén con tu procedencia y se te paga desde el tesoro, **nunca más de lo que tiene** en ese momento.
- Completo → `FULFILLED`; vencido → `EXPIRED` (o `FAILED` si se entregó algo). `ContractCreatedEvent`, `ContractResolvedEvent`. Los resueltos se olvidan a los 30 días.

Pruebas: `contractsAreDeliveredAndPaidOrExpire`, `peopleConsumeAndScarcityRaisesPrices` (contrato por escasez).
