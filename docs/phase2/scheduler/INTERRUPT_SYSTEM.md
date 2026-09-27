# Interrupt System

Clases `interrupt.{InterruptPolicy, InterruptStack, InterruptFrame}`.

## Políticas

- `CANCEL`: se descarta (nunca entra en la pila).
- `PAUSE`: conserva el progreso y se reanuda hasta `pauseMaxTicks`=6000.
- `SUSPEND`: como PAUSE pero solo si la interrupción fue corta (`suspendMaxTicks`=2400); si no, caduca.
- `RESUME`: continúa donde iba, sin límite.
- `RESTART`: vuelve a empezar (`restarts`++).

Cada rutina trae la suya en su perfil (ver [ROUTINE_ENGINE](ROUTINE_ENGINE.md)); las respuestas se cancelan siempre.

## Pila

Profundidad máxima `maxInterruptDepth`=4 (desborda tirando la más antigua). Al terminar la interrupción, tras `resumeDelayTicks`=20, se reanuda la última pila **si nada de capa superior compite**; los marcos caducados se descartan y se publican (`RoutineCompletedEvent` "lapsed"). La reanudación **vuelve a resolver el lugar** (el mundo pudo cambiar).

## Escenario clave (verificado en servidor real)

patrulla → sonido → `RoutineInterruptedEvent(PATROL, por INVESTIGATE, PAUSE)` → investigar (el samurái camina hacia el ruido) → el disparador cesa → `RoutineCompletedEvent(INVESTIGATE, "trigger gone")` → `RoutineStartedEvent("resumed after INVESTIGATE (PAUSE)")` → vuelve a patrullar.

Pruebas: `PriorityInterruptTest` (5 de pila) y `aPatrolInterruptedBySoundInvestigatesItAndThenReturnsToThePatrol`, `anNpcThatCannotWalk…`; físicas `aPatrolBrokenBySoundInvestigatesAndReturns`, `fearOverridesTheNightsSleep`.
