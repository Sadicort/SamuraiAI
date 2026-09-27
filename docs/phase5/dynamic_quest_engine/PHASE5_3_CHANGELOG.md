# Changelog — Fase 5.3 Dynamic Quests

## Añadido

- `living/quest`: 22 tipos de condición con clave estable, generador (condición → análisis → plantilla → variables → misión), 21 plantillas integradas y plantillas por configuración, expresiones, 5 etapas de historia con giros, fusiones y resolución por el mundo, 11 tipos de objetivo con señales reales, 6 caminos, 6 tipos de recompensa pagados por la economía real, 14 tipos de consecuencia aplicados por el motor dueño, 5 campañas, cadenas, historia por jugador, diálogo según emoción y confianza, 9 eventos, métricas, inspector, persistencia.
- Adaptador: señales de jugadores (llegar, meditar, matar, hablar, entregar), anuncios de misiones, comandos.

## Corregido en la integración

- Una condición de otro recurso se fusionaba en la misión de comida y la resolvía.
- Aceptar una oferta caducada ya no es posible.
- Las condiciones `DISPUTE` y `GRUDGE` no las informaba nadie: la plantilla de disputa y la campaña «Viejas rencillas» no podían aparecer. Ahora salen de las relaciones cognitivas y de las familias rivales.
- La variable `{season}` llegaba en inglés (`SPRING`); ahora en español.
- Dos nombres de experiencia no existían en el catálogo cognitivo y se descartaban en silencio: `BETRAYED_ME` (consecuencia de relación negativa → ahora `BETRAYED`) y `WITNESSED_HEROISM` (testigos de una defensa → ahora `HONOR_OBSERVED`). Una prueba de arquitectura valida ahora todos los nombres usados.
