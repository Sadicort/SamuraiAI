# Household Engine

**Código:** `living/family/household/Household.java`; `FamilyEngine.household`.

- Un **hogar no es una familia**: una familia puede tener varias casas y una casa puede acoger a gente de varias familias.
- Apunta a la casa de la aldea (`HomeRecord.house`, el mismo sistema de casas) y a las cuentas de la economía; conoce residentes, jefe, camas de la casa, visitantes, horario y estado (`NORMAL, CROWDED, EMPTY, ABANDONED, DAMAGED, DESTROYED, RELOCATING`).
- **Agrupación** (`groupHouseholds = true`): un NPC que llega a una casa donde ya vive alguien con familia se une a esa familia según la diferencia de edad con ese residente: **pareja** si ninguno tiene pareja y se llevan ≤ `partnerAgeGap` (15) años, **hermano/a** (mismos padres, creados si hace falta) si se llevan ≤ `siblingAgeGap` (12), **descendiente** si es ≥ `parentAgeGap` (18) años más joven; si no encaja en ninguno, funda su propia familia.
- Cada día se actualizan camas y estado desde la aldea (casa dañada o destruida).

Prueba: `householdsGroupPeopleIntoKinshipByAge`.
