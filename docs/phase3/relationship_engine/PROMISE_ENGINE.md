# Promesas

`PromiseRecord(promitente, destinatario, kind ESCORT|PROTECT|DELIVER|MEET|TEACH|DUEL|OATH|GENERIC, asunto, madeAt, dueAt, pública, peso, estado ACTIVE|FULFILLED|BROKEN|EXPIRED)`.
`promise/fulfill/breakPromise` (+ expiración en `tick`). Consecuencias sobre el promitente ≠ NPC: cumplida (+confianza 12, respeto 6, honor 8…), rota (−confianza 30, −honor 25, −lealtad 12, +rivalidad), expirada (−confianza 8). Eventos `PromiseCreated/Fulfilled/Broken`. Comandos `/samuraiai relationship promise|promises|fulfill|break`.
