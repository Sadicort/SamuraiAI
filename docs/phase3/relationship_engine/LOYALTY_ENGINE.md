# Lealtad

Crece lentamente y sólo sobre confianza (`loyaltyMinTrust` 55; por debajo la ganancia se multiplica por 0,2). Tipos: PERSONAL, FACTION, FAMILY, MASTER, STUDENT, GUARD. Un golpe ≥ `loyaltyBreakDelta` la **rompe** (`loyaltyBroken`, `LoyaltyChangedEvent.broken`) hasta que se reconquiste (≥30).
