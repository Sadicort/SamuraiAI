# Depuración de percepción

Comandos (operadores, `/samuraiai perception ...`):

- `status` — resumen de métricas.
- `inspect <npc>` — `PerceptionInspector`: conciencia, atención, amenaza, sospecha, foco (con motivos), objetivos vistos, sonidos oídos, amenazas, interés, objetivo de investigación, entorno, memoria por tipo y estado de cada sensor.
- `sound <x y z> <categoría> <volumen>` — emite un sonido de prueba.
- `debug` — overlay de partículas solo para quien lo pide: **amarillo** campo visual, **verde** visto, **naranja** tapado, **gris** memoria, **cian** sonido, **rojo** amenaza, **dorado** objetivo de investigación, más un marcador de color sobre cada NPC según su conciencia.
- `metrics reset`.

`perception.debugLogging` activa trazas del motor.

## Verificado

Los comandos `status`, `inspect` y `sound` responden en servidor real (`perceptionCommandsRespond`). **El overlay de partículas no se ha verificado visualmente** (no hay cliente en las pruebas); su lógica de dibujo se ejercita sin errores pero el resultado gráfico queda pendiente de revisión humana.
