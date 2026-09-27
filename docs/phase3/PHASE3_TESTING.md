# Pruebas

Unitarias (`src/test/.../ai/cognition`): memoria (18), relaciones (19), emoción (14), conocimiento (11), sociedad (8), rumores/propagación (12), escenarios de integración e **reinicio** (12), almacenamiento (9: escritura segura, corrupción, versión futura, migraciones, dirty, urgente, descarga), rendimiento (1), arquitectura (5), adaptador/Brain (6).
Escenarios de la especificación: 1 rescate, 2 traición, 3 descubrimiento+enseñanza, 4 rumor, 5 trauma, 6 cultura y 7 relación asimétrica.
Físicas (`CognitionGameTests`, 4): experiencias→estado legado y persistencia; evolución de personalidad al Scheduler; rumor entre vecinos; comandos.
Errores reales que los tests destaparon están en cada changelog (p. ej. CME en la reinterpretación, mood que no volvía a NEUTRAL, evidencia directa degradada por el humor).
Verificación final: ver `PHASE3_FINAL_REPORT.md`.
