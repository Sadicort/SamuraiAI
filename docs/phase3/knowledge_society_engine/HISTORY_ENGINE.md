# Historia

`HistoricalEvent(tipo BATTLE|FIRE|FOUNDING|VISIT|DISCOVERY|FESTIVAL|DISASTER|HERO_ACT|BETRAYAL|CONSTRUCTION|DEATH|OTHER, cuándo, lugar, participantes, significancia, kind, traza, comunidad, testigo)`. `HistoryEngine` mantiene una lista ordenada y acotada: al llenarse cae el menos significativo, así permanecen los grandes sucesos. Sólo entra lo ≥ `publicSignificance`. Comunidad + **línea temporal del mundo**.
