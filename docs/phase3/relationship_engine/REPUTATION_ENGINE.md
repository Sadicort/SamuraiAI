# Reputación

`ReputationBook` por NPC: `ReputationRecord(sujeto, ámbito LOCAL|VILLAGE|FACTION|GLOBAL, puntuación por etiqueta PROTECTOR/MERCHANT/SAMURAI/BANDIT/TRAITOR/WISE, confianza, fuentes independientes, directos/rumores)`.
`credibility = (credTrust·confianzaEnFuente + credRepute·reputaciónFuente + credEvidence·evidencia + credRelation·relaciónConSujeto)/Σ + bonus por confirmaciones independientes`. Una **fuente repetida no cuenta dos veces**; una fuente en la que no se confía (< `reputationMinCredibility`) no se cree. Los rumores se traducen a `ReputationHearsay` (lo emite el hub tras entregar un rumor) y matizan la relación con el sujeto (`hearsayInfluence`).
