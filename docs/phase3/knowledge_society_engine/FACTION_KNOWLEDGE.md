# Conocimiento de facción y control de acceso

`AccessLevel` PUBLIC < MEMBERS < INNER_CIRCLE < LEADERS. Cada hecho tiene un nivel; cada miembro un rango por comunidad (`SocietyEngine.rankOf/sharedRank`). `FactionKnowledge.canKnow`, y `PropagationEngine.select` y `teach` respetan el rango: no todos los miembros saben todo.
