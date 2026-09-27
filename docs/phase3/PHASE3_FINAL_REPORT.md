# Informe final — Fase 3.0 → 3.4

## Estado
Implementado y verificado. **`FOUNDATION_STATUS.json` sigue en PARTIAL / `phase2Unlocked: false`**: se implementó por instrucción expresa y no se falsificó el estado.

## Qué es ahora un NPC
Un individuo persistente con memoria estructurada, emociones con causa, relaciones direccionales explicables, conocimiento con fuente y validación, pertenencia a comunidades con cultura, historia y rumores, y personalidad que evoluciona despacio. Reiniciar el servidor no borra su historia.

## Tamaño
278 ficheros Java nuevos (`ai.cognition`, `ai.memory`, `ai.relationship`, `ai.emotion`, `ai.knowledge`), ≈ 12 500 líneas de producción, 5 ficheros `.toml`, 6 familias de comandos, ≈ 130 pruebas nuevas.

## Verificación
`./gradlew check` **sin** CustomNPCs y `check -PwithCustomNpcs=true`: ambos en verde. 404 pruebas unitarias (0 fallos, 1 omitida preexistente), 21/21 GameTests (17 previos + 4 cognitivos), `verifyDistributionJar` correcto. Con CustomNPCs los eventos físicos de golpe/muerte sí encuentran avatar.

## Definition of Done
Memoria (persistente, episódica/semántica/emocional/procedimental/espacial/temporal, consolidación, olvido, fusión, compresión, índices, recuperación, caché) ✔. Relaciones (confianza, respeto, honor, lealtad, afinidad, rivalidad, amistad, reputación, promesas, grafo, persistencia) ✔. Emoción (registro, intensidad, duración, dimensiones, disparadores, ánimo, mezcla, decaimiento, regulación, trauma, recuperación, resiliencia, historial) ✔. Conocimiento (registro, grafo, lugares, personas, objetos, descubrimiento, aprendizaje, enseñanza, validación, enciclopedia) ✔. Sociedad (comunidades, cultura, facciones, rumores, propagación, historia, memoria mundial, conocimiento colectivo) ✔. Runtime (eventos, métricas, debug, almacenamiento, migraciones, caché) ✔.

## Límites conocidos (honestos)
- El coste medido es de simulación con NPC sintéticos; no hay prueba de carga con cientos de NPC físicos en el juego.
- Las **expresiones y la fisiología** son consejos (`CognitiveAdvice`) que hoy sólo se muestran: ningún Behavior/animación los aplica todavía (fuera de alcance: "no saltarse las APIs de movimiento").
- `Temperament`/velocidad de movimiento emocional no se aplica a Navigation.
- Los objetos/economía, familia, embeddings/SQLite, GeckoLib y voz están **preparados, no implementados**.
- El guardado es síncrono en el hilo del servidor (repartido por presupuesto; `saveAll` al parar puede tardar segundos con cientos de NPC).
- Los overlays de depuración no se han verificado visualmente.
- La detección de lugares descubiertos es una heurística (cueva/río/montaña/bosque) y las zonas del Scheduler; no hay detección de estructuras.
- Con NPC sin cuerpo (perfil sin CustomNPCs) los eventos físicos de golpe/muerte no encuentran avatar; las experiencias se prueban por la API.
- El estado de personalidad del Scheduler sigue sin persistirse por sí mismo; se **restaura** desde la evolución guardada del libro de personalidad.
