# Formation Engine

Clases `formation.{FormationType, FormationSlot, FormationEngine}`. Solo geometría: calcula puestos, no mueve a nadie.

## Formas

`LINE` (en fila, de lado a lado), `COLUMN` (en fila india tras el líder), `CIRCLE` (equidistantes), `DIAMOND`, `ESCORT` (uno delante, laterales y uno detrás; el protegido al centro), `TRIANGLE` (el líder en el vértice). Espaciado `formationSpacing`=2.5. El puesto 0 es el del líder.

## Asignación por rol

`assign(type, leader, lx, lz, hx, hz, members, spacing)`: el líder toma el puesto 0; el resto se ordena por rol y toma los puestos restantes de delante hacia atrás, salvo que los **guardias prefieren los flancos** (mayor |right|) y la **reserva** toma lo más atrasado. El rumbo (hx,hz) gira toda la forma; sin rumbo se usa +z.

## Rumbo

`GroupCoordinator` lo estima del movimiento del líder entre sincronizaciones (> 0.5 bloques) o, si va de camino, hacia su destino. Parado: `standing()` (círculo en casi todos los tipos).

## Cómo llega al mundo

El puesto se convierte en `Place` (radio ≈ 0.4×espaciado) dentro del `SchedulerAdvice`; el Brain lo entrega a `RoutineTask`, que pide a Navigation ir allí y **sigue el puesto si se mueve**. La validez del terreno la resuelve Navigation. Si el seguidor está a > 10 bloques del puesto, corre.

Pruebas: cada forma tiene un puesto por miembro sin solaparse; geometría característica; roles y rotación por rumbo.
