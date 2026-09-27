# Trauma

`TraumaEngine.consider`: un disparador traumático o muy fuerte crea (o **profundiza**, si coincide un desencadenante) un `TraumaRecord`: origen, emoción, profundidad, recuerdos, fecha, progreso de recuperación, fase (ACTIVE/RECOVERING/RECOVERED) y **desencadenantes** (`entity:`, `cell:`, `zone:`, `kind:`). Las emociones desagradables ligadas usan curva TRAUMA. Los *flashbacks* (`flashbacks()`) son sólo emociones con cooldown. Los campos `flashbacks`/`avoidance` son la estructura preparada para un futuro TEPT; **no hay comportamiento extremo**.
