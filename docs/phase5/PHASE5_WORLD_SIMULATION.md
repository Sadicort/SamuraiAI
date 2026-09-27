# Fase 5 — Simulación del mundo

## Cada tick (`LivingWorld.tick`, desde `LivingService.tick`)

1. **Calendario:** avanza el reloj (sol o `gameTime`), cambios de fase, clima de celdas que toca.
2. **Mundo:** streaming (cada 40 ticks), eventos de mundo que toca, regiones cuyo paso toca (cada 5 ticks, máx. 4, 1,5 ms).
3. **Una etapa del trabajo del día** si hay un día pendiente (eventos espontáneos → familias → comercio → condiciones de misión, una por tick).
4. **Un bucket** (`buckets` = 5): aldeas / economía (caravanas) / misiones / mercados / libre.
5. **Guardado** de secciones sucias cada `saveIntervalTicks` (1200), como máximo `maxSavesPerTick` (2).

## Cerca de jugadores (LOD 0, `FULL`)

Los NPCs reales viven con su Brain, navegación y scheduler; el mundo vivo aporta: sesgo de rutinas (horario de aldea, oficio, eventos, clima, seguridad), cama como hogar, interacciones físicas (fuegos, cultivos), horas de trabajo reales como producción, percepción y combate como amenaza, conversación/posición/muertes/entregas como progreso de misión, noticias de la aldea, HUD del calendario, «TU MUNDO» en los prompts.

## Lejos de jugadores (LOD 1–4)

Sin entidades ni chunks: la región avanza en pasos (1 h, 6 h, 1 día, 1 semana) donde la aldea acredita horas planificadas y visitantes, la economía produce, consume, se deteriora y fija precios, la fauna crece, y las caravanas viajan en segundo plano. Al volver un jugador, la región se pone al día con como mucho 48 pasos.

## Ejemplo de extremo a extremo (probado en `LivingWorldTest.endToEndTheWorldLivesPersistsAndComesBack`)

Crear región → fundar aldea → llegan NPCs (ciudadanos, oficios, casas, familias) → mercado → producir → consumir → escasez → misión con causa, dador y lugar → avanzar el calendario → entregar y completar (paga el tesoro, cambia la aldea) → la economía cambia → la familia registra historia → guardar → reiniciar → todo vuelve coherente y sigue viviendo.
