# Priority Engine 2.0

Clase `priority.PriorityEngine`, `PriorityLayer`, `Selection`.

## Cuatro capas

`BASELINE` (la rutina del reloj) < `PERSONAL` (necesidades y ánimo) < `SITUATIONAL` (algo ocurre alrededor) < `EMERGENCY` (supervivencia). Umbral para poder actuar: 20 / 30 / 35 / **70**.

## Selección

Se resuelve entre **todos** los candidatos que superan el umbral de su capa (así el evento de conflicto cuenta también los perdedores de capas inferiores): capa más alta primero; el `ConflictResolver` decide dentro de la capa.

## Emergency Override

Un candidato EMERGENCY sobre el umbral **anula todo**: la histéresis, el tiempo mínimo de permanencia y las políticas de interrupción de lo que se hacía (la rutina se apila según su política o se cancela, pero nada retrasa la emergencia).

## Estabilidad

Se mantiene lo que se hace salvo que hable una **capa superior**, o un rival de la misma capa lo supere en `switchMargin`=8 tras `minRoutineTicks`=400. Si lo actual deja de cumplir su umbral, toma el relevo el mejor que sí. `stickiness`=10 evita vaivén.

Pruebas: `PriorityInterruptTest` (capa gana con menos puntos, umbral, emergencia, histéresis, dwell, relevo).
