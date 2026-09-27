# Modelo de datos familiar

| Registro | Clase | Contenido |
| --- | --- | --- |
| Persona (viva o histórica) | `model/Person` | nombre (`NameRecord`), género para las palabras (`KinGender`), familia de nacimiento y actual, hogar, generación, nacimiento (y si es estimado), lugar de nacimiento, muerte, estado de vida, rol, posición de sucesión, profesión, con/sin cuerpo, etiquetas de legado |
| Parentesco | `genealogy/GenealogyGraph` | solo `PARENT_OF` y `PARTNER_OF` |
| Familia | `registry/FamilyRecord` | ver `FAMILY_RUNTIME.md` |
| Hogar | `household/Household` | residentes, jefe, casa (de la aldea), camas, estado |
| Linaje | `lineage/Lineage` | escuelas, oficios, líneas |
| Maestro/discípulo | `mentorship/Mentorship` | aprendizaje |
| Técnica | `knowledge/Technique` | saber que debe enseñarse |
| Herencia | `inheritance/InheritanceRecord` | historia de propiedad |
| Reliquia | `heritage/Heirloom` | objeto con historia |
| Nacimiento | `parenthood/BirthRecord` | acta de nacimiento |
| Legado | `legacy/LegacyRecord` | causas de lo que deja |
| Memoria familiar | `family_memory/FamilyMemoryEntry` | lo que la familia cuenta de sí |

Una persona **nunca es una entidad**: los antepasados históricos son registros ligeros; nunca se cargan como entidades.
