# Kinship Engine

**Código:** `living/family/kinship/KinshipEngine.java`; `FamilyEngine.kinship/kinshipDegree/isRelated/commonAncestor`.

Nombra el parentesco entre dos personas **derivándolo** del grafo: padre/madre/progenitor, hijo/hija, hermano/hermana (y medio hermano/media hermana si comparten un solo progenitor), abuelo/abuela, nieto/nieta, tío/tía, sobrino/sobrina, primo/prima, pareja, ancestro, descendiente o pariente lejano.

- Las palabras con género solo se usan si el registro de la persona lo dice (`KinGender`); si no, la neutra («progenitor», «hermano/a»…). A los NPCs existentes **no** se les adivina el género por el nombre: quedan `UNSPECIFIED`.
- El género **nunca** afecta a sucesión, herencia ni jefatura.
- `kinshipDegree` (grados de separación), `commonAncestor` (ancestro común más cercano).

Prueba: `kinshipIsDerivedAndNamedWithoutGuessing`.
