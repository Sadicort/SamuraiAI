# Disparadores

`EmotionTrigger(npc, fuente, efectos, recuerdo, ref, traza, at, lugar, entidad, traumático, peso, fromEcho, kind, nota)`. Fuentes: PERCEPTION, MEMORY, RELATIONSHIP, WEATHER, TIME, COMBAT, CONVERSATION, VOICE, GLOBAL_EVENT, OBJECT, ECHO, CONTAGION, ADMIN.
`TriggerEngine`: intensidad = efecto · (0,5+0,5·peso) · ganancia de personalidad · (desagradable ? 1/resiliencia : 1). Misma clave (tipo+origen) → **fusión** (`fusionGain`), no duplicado. Al ser desagradable y fuerte (≥ `traumaThreshold`, peso ≥ `traumaMinWeight`) o marcado traumático → trauma. Un evento placentero ayuda a recuperar.
