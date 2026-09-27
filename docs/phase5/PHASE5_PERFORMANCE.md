# Fase 5 — Rendimiento

## Diseño

| Prohibición | Cómo se cumple |
| --- | --- |
| actualizar todas las aldeas cada tick | bucket de aldeas cada 5 ticks, `tickIntervalTicks` 20, máx. 8 aldeas por pasada |
| simular caravanas físicamente | aritmética de progreso por paso, sin entidades |
| correr todas las misiones cada tick | bucket de misiones; caducidades cada 20 ticks |
| recorrer la genealogía cada tick | familias una vez al día; cachés de ancestros por versión |
| escribir disco cada tick | solo secciones sucias, 2 por guardado, cada 1200 ticks |
| simular regiones lejanas al detalle | LOD de 5 niveles, pasos de 1 h a 1 semana, presupuesto 1,5 ms, puesta al día ≤ 48 pasos |
| todo el trabajo del día en un tick | 4 etapas en 4 ticks consecutivos |
| clima en cada celda cada tick | cola de prioridad por próximo cambio |
| depósitos y fauna por tick | regeneración perezosa al leer; logística en forma cerrada |

## Medido (`LivingPerformanceTest`, JVM de pruebas, Windows 11, sin Minecraft)

Carga: 10 aldeas planificadas × 20 ciudadanos (200 ciudadanos, ~830 personas con antepasados), 13 regiones, un jugador en la primera aldea, **10 días de Deiliora a velocidad real (240 000 ticks)**.

| Medida | Valor |
| --- | --- |
| tick medio | ~4,5 µs (calendario 0,2–0,5; mundo 1,5; aldeas 2,1; economía 0,15; misiones 0,1 µs) |
| trabajo del día (4 etapas) | ~5–6 ms repartidos: eventos 0,5 ms, familias 2,2 ms, comercio 2,7 ms, condiciones de misión 0,7 ms |
| tick más caro en régimen estable (desde el día 1) | 3–5 ms |
| picos de arranque (primeros ticks y primera vez de cada camino de código: carga de clases, JIT) | hasta ~50 ms |
| 10 días completos | ~1 s de CPU |

Antes de repartir el día en etapas, la medianoche costaba 5–7 ms en un solo tick con esta carga (y crecería con la población); ahora la etapa más cara es la de comercio o familias por separado.

## En el servidor

- `/samuraiai living metrics`: coste por tick del hub y de cada motor, días, eventos, reacciones y sus fallos, interacciones físicas.
- El adaptador añade: lista de jugadores por tick (barata), señales de jugadores cada 40 ticks, camas cada 200, zonas cada 600, escaneo de un edificio cada 20 ticks (≤ 17×8×17 bloques en chunks cargados), órdenes de interacción cada 100 ticks.
- Consultas de sesgo del scheduler: una por evaluación del scheduler (que ya está acotada), con plan del día en caché.

## Límites

Las cifras son de una JVM de pruebas en un solo equipo; no se ha medido aún un servidor con 50+ NPCs con cuerpo y varios jugadores repartidos. Los picos de arranque son del JIT y ocurren una vez por sesión.
