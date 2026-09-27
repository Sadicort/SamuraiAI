# Temple Life Engine

**Código:** `living/village/temple/TempleLifeEngine.java`; evento `TempleRitualEvent`.

- **Ritos** al amanecer y al atardecer: mientras dura un rito el templo atrae a los devotos (sesgo `PRAYER` para todos, más fuerte para monjes) y la aldea marca `ritualActive`.
- **Enseñanza** por la mañana: los monjes enseñan (MEDITATE/SOCIAL en el templo).
- **Ceremonias** en días sagrados: un evento de aldea `CEREMONY` (PRAYER +35, MEDITATE +10, WORK −10) hace del templo el centro del día.
- La luna llena y los festivos con etiqueta `temple` vienen del calendario; la misión del rito de luna llena la abre el escáner de condiciones del hub; la economía cobra el diezmo del templo (`templeTithe`).
