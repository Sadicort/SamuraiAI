# Confianza

Difícil de ganar, fácil de perder: ganancias × `trustGainScale` (0,9), pérdidas × `trustLossBias` (1,4). Niveles (`TrustEngine.level`): UNKNOWN (poco contacto y sin movimiento), SUSPICIOUS <25, NEUTRAL <45, TRUSTING <65, CLOSE <94, ABSOLUTE_TRUST ≥94 (umbrales configurables). Inicial 35.
Sube: conversación, ayuda, protección, promesas cumplidas, tiempo compartido, rescates, entrenamiento. Baja: mentiras, ataques, traición, abandono, amenazas, promesas incumplidas.
Evento `TrustChangedEvent` sólo si |Δ| ≥ `eventMinDelta` o cambia el nivel.
