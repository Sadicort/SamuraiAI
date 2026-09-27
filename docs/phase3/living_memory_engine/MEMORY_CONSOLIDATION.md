# Consolidación

`consolidation/ConsolidationEngine` (lotes de `consolidationBatch`):
1. Un recuerdo TEMPORARY con edad ≥ `consolidationDelayTicks` (o cualquiera si el NPC duerme) se **fusiona** con uno consolidado similar (`MemoryMerger`, similitud ≥ `mergeSimilarity`, mismo tipo, dentro de `mergeSpanTicks`, importancia < IMPORTANT) o se **promueve** a CONSOLIDATED (+`consolidationBoost`).
2. Se pliegan sus impresiones a la memoria semántica.

**Dormir** (`MemoryEngine.sleep`): hasta `sleepBatch` recuerdos frescos y refuerzo (`sleepReinforce`) de los importantes/emocionales; acotado, no caro. El adaptador lo llama al terminar la rutina SLEEP.
**Fusión** suma repeticiones, promedia la firma emocional, conserva consecuencias y capítulos.
