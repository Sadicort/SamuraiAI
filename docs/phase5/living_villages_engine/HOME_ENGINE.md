# Home Engine (aldeas)

**Código:** `living/village/homes/HomeEngine.java`, `HomeRecord.java`.

- `assign`: cama en la casa **menos llena** con plazas (las familias se reparten), habitación, zona privada (radio de la casa) y objetos personales según el oficio (p. ej. herramientas para el herrero; siempre «amuleto del templo»).
- `neighbours(aldea, ciudadano, radio)`: quién vive cerca (vida social).
- `release`: libera la cama al irse, morir o destruirse la casa.
- `homeless(aldea)`: ciudadanos sin cama → `HousingShortageEvent` → misión de construir casas (`HOUSING_SHORTAGE`).

Cómo la cama llega al NPC real, cómo se sustituye por la cama física encontrada en la casa y cómo lo relee el scheduler: `../living_world_engine/HOME_ENGINE.md`. La Family Engine agrupa las casas en *households* sin crear otro sistema de casas.
