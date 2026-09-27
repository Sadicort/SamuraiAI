# Family Consistency Validator

**Código:** `living/family/genealogy/ConsistencyValidator.java`.

Rechaza un vínculo padre-hijo que:

- haría a alguien su propio padre;
- invertiría un vínculo existente (A padre de B y B padre de A);
- cerraría un ciclo de ascendencia;
- duplicaría una arista;
- daría a un hijo más de dos progenitores;
- sería imposible en el tiempo (un progenitor con menos de `minParentAge` (16) años al nacer el hijo).

Rechaza una pareja consigo mismo o entre progenitor e hijo. `audit()` revisa todo el almacén (tras cargar y bajo demanda) y devuelve los problemas sin corregirlos.

Prueba: `impossibleGenealogiesAreRejected`.
