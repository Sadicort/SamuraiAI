# Citizen Engine

**Código:** `living/village/citizens/Citizen.java`; `VillageEngine.admit/depart/assignProfession/recordWork`.

## Qué es de la aldea en un ciudadano

Aldea, estado (`RESIDENT, VISITOR, MIGRATED, MISSING, DECEASED`), desde cuándo, oficio y **horas de oficio** (experiencia), casa, roles en la comunidad, desfase personal del horario y `embodied` (si tiene entidad). Edad y familia son de Family; relaciones, emociones y recuerdos de la capa cognitiva.

## Ciudadanos sin cuerpo

`embodied = false` para quien existe solo en la simulación (nació o llegó mientras la región era abstracta, o su NPC está descargado porque el servidor se apagó): vive, trabaja y consume como los demás, pero no tiene entidad hasta que el adaptador le da una.

## Entrada y salida

- `LivingWorld.npcArrived` (NPC activado): se une a la aldea donde está, o a la más cercana en `villageJoinRadius`, o funda una (`autoVillages`). `admit` le da oficio (`ProfessionAssigner`), casa, desfase personal (de la diligencia cognitiva ± desfase determinista) y se une a la comunidad de Knowledge. Si ya era ciudadano de otra aldea, **migra** (sale de la anterior). `CitizenJoinedEvent` → familia, puesto de mercader, registro de población.
- `depart`: `DECEASED` (muerte real en el mundo, `LivingDeathEvent` del cuerpo del NPC), `MISSING` (eliminado por comando o avatar desaparecido), `MIGRATED`. Se libera la cama y el asiento en la comunidad; el registro se conserva como historia. Al apagar el servidor el ciudadano **no** se va: solo pasa a `embodied = false`.

## Trabajo

`recordWork(npc, horas)` suma horas de oficio (rutinas reales completadas) y la simulación abstracta acredita las planificadas. La Economy convierte el **incremento** de horas en producción; `Skill` da el rango y el multiplicador.
