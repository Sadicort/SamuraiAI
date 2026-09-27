# Personalidad × emoción

Reglas configurables `TRAIT|EMOTION|G|D|factor` (`emotion.personalityRules`, por defecto: curiosos reaccionan más a la novedad, valientes sienten menos miedo, pacientes sueltan la ira antes, orgullosos sufren más y más tiempo la vergüenza…). Multiplicador `1+factor·lean(rasgo)`. Nada hardcodeado en código. **Tres capas de personalidad** (`PersonalityLedger`): base, evolución a largo plazo (tasa, tope y límite diario), y modificadores emocionales temporales (`TemporaryModifiers`: el miedo enmascara el valor sin borrarlo).
