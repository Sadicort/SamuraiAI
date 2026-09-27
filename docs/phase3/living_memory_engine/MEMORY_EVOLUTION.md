# Evolución de recuerdos

`evolution/MemoryEvolutionEngine`:
- **Relajación emocional**: la intensidad decae hacia 0 (semivida `evolutionHalfLifeTicks`, ×3 para orgullo/gratitud/esperanza, ×0,7 para miedo/ira); un recuerdo **traumático** conserva un residuo `traumaResidual·emotionalWeight`.
- **Confianza** deriva hacia abajo `confidenceDriftPerDay` salvo confirmación.
- **Reinterpretación**: una experiencia HIGH+ nueva con la misma persona y valencia opuesta desplaza (`reinterpretShift`) la valencia de recuerdos anteriores y les añade el tag `reinterpreted:<id>` y una `Consecuencia`. (Una amistad recordada tras una traición ya no se recuerda con calidez.)
