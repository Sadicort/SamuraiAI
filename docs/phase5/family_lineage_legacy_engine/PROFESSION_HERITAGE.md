# Profession Heritage

**Código:** `living/family/profession/ProfessionHeritage.java`; `FamilyEngine.professionSuggestions`.

**La profesión nunca se copia automáticamente.** Se calcula una lista ordenada de oficios sugeridos con razones:

- **exposición:** el oficio de los progenitores (+0,5; el hijo del herrero crece junto a la fragua), el de los demás residentes del hogar (+0,15), el del maestro;
- **tradición** familiar de oficio (`oficio:<x>`);
- **afinidad de personalidad** con cada oficio (rasgos cognitivos: `Outside.affinity`);
- **necesidades** de la aldea (el oficio que más necesita la economía).

`/samuraiai living family suggest <npc>` (op). El oficio real lo asigna la aldea (`ProfessionAssigner`) o un aprendizaje completado.

Prueba: `professionsAreSuggestedNotCopied`.
