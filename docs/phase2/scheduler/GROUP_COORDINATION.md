# Group Coordination

Clases `group.{Group, GroupType, GroupRole, GroupCoordinator, GroupOrder, Alarm, MemberSnapshot}`.

## Tipos y roles

`GroupType`: `PATROL, GUARD, MERCHANT, VILLAGE, SQUAD`. Cada tipo define qué rutinas del líder **comparten** los miembros y su formación en marcha/parado (p. ej. PATROL: columna / línea). Roles: `LEADER, GUARD, SCOUT, RESERVE` con multiplicadores de respuesta (los exploradores investigan más, los guardias asisten más, la reserva huye más).

## Formación automática

Cada `AUTO_GROUP_EVERY`×`groupSyncTicks`: los NPC sin grupo cuyo estilo de vida declara `group=…` se unen a un grupo de su tipo con algún miembro a ≤ `autoGroupRadius`=24, o forman pareja con otro cercano. Máximo `groupMaxSize`=6. Un NPC que se aleja > 2.5× el radio sale. Un grupo de uno se disuelve. Los grupos creados a mano (`/samuraiai scheduler group create`) están **fijados** y no se reordenan.

## Líder y roles

Líder = mejor media de lealtad, disciplina, coraje, paciencia y diligencia + antigüedad. **Reelección** solo si el líder falta, o pasado `leaderReelectTicks`=200 y otro supera al actual en 8 puntos (`GroupLeaderChangedEvent`). Roles por rasgos: 25 % exploradores (curiosidad+coraje), 50 % guardias (coraje+agresión), el resto reserva.

## Órdenes

Si el líder hace una rutina compartida y no está en emergencia, cada seguidor recibe un `GroupOrder` con la misma rutina y **su puesto de formación**. En el NPC se convierte en un candidato `Source.GROUP` cuya puntuación reemplaza al propio (cohesión según disciplina y lealtad: un NPC rebelde puede no seguir).

## Alarmas

Un miembro con amenaza o herido (y sesgo de alarma suficiente) lanza `Alarm`; los demás, si están a ≤ 2×`groupAlertRadius`, reciben candidatos de asistencia (los que pueden pelear; nivel 3 → emergencia), huida o vigilancia; la zona donde ocurre se marca en alerta y se fuerza reevaluar al grupo.

Pruebas: `ZoneFormationGroupTest` (formación, líder, roles, órdenes, alarma, fijados) y `SchedulerRuntimeTest` (`guardsFormAGroupWithALeaderAndFollowInFormation`, `whenTheLeaderIsRemovedAnotherTakesOver…`, `oneGuardRaisingTheAlarm…`); físico `guardsPatrolAsAGroupAndReelectALeader`.
