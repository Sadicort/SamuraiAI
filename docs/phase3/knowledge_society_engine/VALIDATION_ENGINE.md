# Validación

Estados: UNKNOWN, RUMOR, LIKELY, VERIFIED, FALSE, FORGOTTEN. Evidencias (`ValidationEvidence.Kind`): observación directa (confirma/contradice), fuente independiente, evento público, declaración de jugador, fuente fiable, fuente contradictoria. Reglas: una fuente repetida cuenta **una vez**; ≥ `independentForLikely` fuentes → LIKELY; **sólo** evidencia directa/pública/fiable puede dar VERIFIED; un verificado sólo cae por contradicción directa; contradicción con confianza ≤ `falseAt` → FALSE. Cada cambio deja una `Revision`.
