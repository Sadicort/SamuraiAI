# Escaleras, escalas y desniveles

Clases: `graph.NavigationGraph` (aristas), `terrain.HeightAnalyzer`, `movement.PathFollower`.

| Situación | Arista | Movimiento |
|---|---|---|
| Escaleras/losa (subir o bajar 1) | `STEP` | sin salto; el `MoveControl` sube el escalón |
| Bloque completo de 1 | `JUMP` | `JumpControl` cuando la distancia ≤ 1,8 y el cuerpo está en suelo |
| Caída 1..`maxDrop` | `DESCEND` | se camina al borde y se cae; coste creciente, penalización desde 3 |
| Escalera de mano | `CLIMB` | empuje vertical (`MovementBody.climb`) mientras el nodo esté arriba/abajo |

`maxDrop` por perfil (defecto 3; guardia y mercader 2). Una caída mayor no se genera (`dropBeyondToleranceIsRefusedButAllowedWhenWalkerAcceptsIt`).

## Altura

`HeightAnalyzer.analyze` resume ascenso/descenso, saltos, escalones, escaleras y caídas de una ruta; `/samuraiai nav inspect` lo muestra.

## Verificación

- Simulador: `jumpsOntoAPlatformAndClimbsALadder`, `walksUpStairsWithoutJumping`.
- Servidor real con CustomNPCs: `jumpsOntoALedgeAndBackDown` (sube saltando y baja). **Las escaleras de mano y las escaleras de piedra están validadas solo en el simulador**, no en un GameTest físico.
