# Cultura

Datos, no código: líneas `id|nombre|tradiciones|normas` (`knowledge.cultures`; por defecto village, temple, market, guard). Tradición: `nombre~KIND~PERIODO~TIPO_ZONA~fuerza`; normas: `HONOR_SCALE`, `OATH_WEIGHT`.
- `SocietyEngine.honorScale/oathWeight(npc)`: la cultura modifica la **interpretación del honor** (Relationship).
- `dueTradition(npc, periodo, zona)` + `observeTradition`: reconoce el ritual del momento y lo **refuerza**; sin práctica se erosiona (`traditionDecayPerDay`).
- `CognitiveAdvice.ritual/ritualZoneKind` → el Brain suma bonus a MEDITATE/PRAYER/GUARD/TRADE/TRAINING (Culture → Behavior).
