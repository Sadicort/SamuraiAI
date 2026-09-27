# Holiday Engine — festivos fijos

**Código:** `living/calendar/holidays/HolidayEngine.java`, `HolidayDef.java`.

Festivos de fecha fija en líneas como `obon;name=Obon;month=7;day=13;days=3;kind=MEMORIAL;tags=ancestors,family`.

| Integrado | Fecha | Días | Tipo | Etiquetas |
| --- | --- | --- | --- | --- |
| Año Nuevo (Shōgatsu) | 1 de Mutsuki | 3 | `NEW_YEAR` | new_year, family, temple |
| Obon (días de los ancestros) | 13 de Fumizuki | 3 | `MEMORIAL` | ancestors, family, temple |

- `beginningOn(fecha)` devuelve los que empiezan ese día → `HolidayEvent(id, nombre, tipo, etiquetas)`.
- El Año Nuevo se anota en la cronología («Comienza el año N»).
- **Gancho familiar:** `LivingReactions` llama `families.ancestorsHonoured()` al recibir un festivo con la etiqueta `ancestors`: las familias recuerdan a sus ancestros (memoria familiar).
- Las etiquetas permiten que otros motores reaccionen sin conocer los ids.

Configuración: clave `holidays` de `samuraiai-calendar.toml` (vacía = integrados).
