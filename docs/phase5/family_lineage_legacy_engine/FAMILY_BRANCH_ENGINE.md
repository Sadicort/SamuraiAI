# Family Branch Engine

**Código:** `FamilyEngine.branch`; evento `FamilyBranchCreatedEvent`.

- Una familia puede **dividirse en una rama nueva** fundada por uno de sus miembros (por migración, disputa, un nuevo hogar…). La rama es una familia con su propio registro (mismo apellido, etiqueta `rama`) que recuerda a la familia madre; el fundador **y sus descendientes** que estaban en la madre pasan a la rama, el fundador es su jefe, y si era el jefe de la madre ésta elige otro por sucesión. Ambas anotan la división en su memoria.
- Disparador hoy: `/samuraiai living family branch <npc>` (op) o API. Las ramas no se crean solas todavía.
- Una rama extinguida conserva su historia.
