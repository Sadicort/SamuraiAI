# Rendimiento

Medido (tests, JVM de desarrollo): **≈ 314 µs por experiencia** (todos los motores), **≈ 20 µs por tick de mente**, consulta de memoria con 3 000 recuerdos < 2 ms, consulta de grafo social < 300 µs, guardado 1 500 archivos ≈ 3,4 s.

Control de coste:
- Mente de cada NPC con intervalo por **distancia al jugador más cercano** (`nearInterval` 20 → `hibernateInterval` 1 200), tope `maxMindsPerTick` y presupuesto `budgetMicros`; los más atrasados primero.
- Memoria: índices, caché corta/caliente/tibia, consolidación y olvido por lotes, compresión por presupuesto de grupos.
- Relaciones: decaimiento por lotes con cursor; sólo eventos cuando cambia algo real.
- Emociones: sólo las activas (≤ `maxActive`); historial acotado.
- Conocimiento: índices; olvido por lotes; grafo con `queryLimit`.
- Sociedad: propagación por cola acotada y **presupuesto por tick**; escaneo social limitado a 24 pares.
- Persistencia: dirty + barrido con tope + urgentes.
La reducción de frecuencia no pierde continuidad: todo decaimiento se calcula desde marcas de tiempo del mundo.
