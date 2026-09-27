# Regulación

`RegulationEngine`: eficiencia = 0,6+0,4·disciplina; cuando la carga desagradable supera `regulationThreshold` el NPC "desea" una técnica según su carácter (BREATHE/MEDITATE/TALK/ISOLATE/SLEEP) → `RegulationAdvice` (con urgencia). Lo que *regula de verdad* es la actividad (el adaptador la deduce de la rutina: SLEEP→dormir, MEDITATE/PRAYER→meditar, SOCIAL→hablar). Preparado para animaciones futuras.
