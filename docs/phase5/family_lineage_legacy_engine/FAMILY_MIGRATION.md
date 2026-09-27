# Family Migration

**Código:** `FamilyEngine.migrated`; hub `LivingReactions.citizenJoined`.

- Cuando el **jefe** de una familia llega como ciudadano a otra aldea (el adaptador lo detecta al activarse el NPC en otro lugar; la aldea anterior lo registra como migrado), la familia cambia de aldea: estado `MIGRATED`, memoria («La familia se traslada»), `FamilyMigratedEvent`.
- Los demás miembros siguen siendo ciudadanos de donde estén; la familia es un registro, no arrastra cuerpos.
- Si se va un miembro que no es el jefe, solo se anota su marcha.

Prueba: `LivingWorldTest.aMasterOfATradeKeepsItsSecretAndAFamilyMovesWithItsHead`.
