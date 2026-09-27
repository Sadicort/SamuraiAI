# District Engine

**Código:** `living/village/districts/District.java`, `DistrictKind.java`.

Distritos: `RESIDENTIAL`, `MARKET`, `AGRICULTURAL`, `MILITARY`, `SPIRITUAL`, `CRAFTS`, `OUTER_FOREST`. Cada tipo de edificio pertenece a uno (`BuildingKind.district()`: casa, cocina y pozo → residencial; plaza, mercado y almacén → mercado; granja, muelle y establo → agrícola; dojo, puesto y puerta → militar; templo → espiritual; herrería, carpintería y taller → artesanal; mina y aserradero → bosque exterior).

`District` guarda sus edificios y su **actividad** 0..1: personas presentes sobre capacidad a detalle completo, u ocupación planificada por los horarios cuando la aldea se simula en abstracto. La actividad alimenta la vida social y la depuración (`/samuraiai living village info`).
