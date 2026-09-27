# Templos (resumen en el mundo)

Detalle: `../living_villages_engine/TEMPLE_ENGINE.md` (`living/village/temple/TempleLifeEngine.java`).

- Un templo es un edificio `TEMPLE` de una aldea (planificado, registrado por comando o importado de una zona `TEMPLE` del scheduler) o un asentamiento de tipo `TEMPLE` (sin mercado, con residentes, cultura `temple`).
- Vive del calendario: la luna llena (`MOON_ENGINE.md`) abre la misión de rito cuando se acerca; los festivales con `PRAYER` llenan el templo; Obon y Año Nuevo llevan la etiqueta `temple`.
- Los monjes (`PRAYER`) y sanadores trabajan allí; la economía cobra el diezmo (`templeTithe`) a una cuenta del templo.
- Región de tipo `TEMPLE`: la de menos peligro del catálogo (0,01).
