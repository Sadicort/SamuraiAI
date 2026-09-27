# Memoria semántica

`semantic/SemanticMemory` guarda **creencias estables** sobre personas y lugares (`Aspect`: TRUSTWORTHY, DANGEROUS, FRIENDLY, HELPFUL, HONORABLE, SAFE, SACRED, HOSTILE), con valor −1…+1, confianza y soporte.

- Se alimentan al **consolidar**: cada `Impression(aspect, delta)` del recuerdo se pliega una sola vez (`semanticApplied`).
- "Yeremi es confiable" no depende de recordar una conversación concreta.
- No es *Knowledge*: son impresiones del NPC; Knowledge guarda hechos con fuente y validación.
- Los recuerdos comprimidos (gist) son de tipo `SEMANTIC`.
