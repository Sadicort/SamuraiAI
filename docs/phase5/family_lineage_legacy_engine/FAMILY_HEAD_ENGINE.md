# Family Head Engine

- Cada familia tiene un **jefe** (`FamilyRecord.head`) y opcionalmente un **sucesor designado**.
- El fundador es el primer jefe; al unirse gente a la familia el jefe se conserva.
- Si el jefe muere, desaparece o pasa a la historia, o si la familia amanece sin jefe vivo, se ejecuta la **sucesión** (`SUCCESSION_ENGINE.md`) → `FamilyHeadChangedEvent`.
- `designateHeir(familia, persona)` (comando `/samuraiai living family designate <npc>`) da un gran peso a esa persona en la sucesión (`designation = 3`).
- El jefe de una familia que se muda a otra aldea se lleva a la familia (`FAMILY_MIGRATION.md`).
