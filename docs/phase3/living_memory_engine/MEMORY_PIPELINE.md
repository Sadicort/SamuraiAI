# Memory Intake Pipeline

```
Experience → MemoryEvaluator (score, importancia, keep?) → ¿repetible y ya existe? → reforzar
                                            └→ MemoryClassifier (tipo) → MemoryRecord TEMPORARY → índices
                                                → (más tarde) Consolidation → CONSOLIDATED
```
1. **Experience** (`memory/model/Experience`): actor, objetivo, participantes, lugar, `Stamp`, resultado, firma emocional, eventos, origen (`Origin` con `traceId`), contexto, consecuencias, impresiones. Inmutable.
2. **Evaluación** (`MemoryEvaluator`): `score = wBase·magnitud + Σ wᵢ·factorᵢ`. Factores: duración, rareza (cuántas veces ha vivido algo igual), emoción, relevancia para el objetivo actual, participantes, lugar (novedad/zona), consecuencias, peso de la relación con el actor, rasgo de personalidad afín a la categoría, peligro. Cada factor queda en `Evaluation.factors` (auditable).
3. Un evento **traumático** es al menos CRITICAL; uno **pivotal** (traición, muerte presenciada) es CRITICAL si se sintió (emoción ≥ 0,5).
4. `keep = score ≥ retainThreshold` (0,10) o traumático. Lo demás se **descarta** (métrica `discarded`): no todo se recuerda.
5. **Repetición**: si la experiencia es `repeatable` y hay un recuerdo casi idéntico dentro de `reinforceWindowTicks`, se **refuerza** (`repeatCount++`, fuerza, importancia) en vez de crear otro.
6. Efectos colaterales: nodo espacial (`context.landmark`), habilidad (`context.skill`), peligro/seguridad del lugar, reinterpretación de recuerdos previos del mismo actor.
