# Grafo de conocimiento

`KnowledgeGraph` (vista sobre los índices): nodos = sujetos/objetos; aristas = registros con objeto y `Predicate` (KNOWS, PROTECTS, LIVES_AT, BELONGS_TO, TEACHES, LEARNED_FROM, VISITED, HEARD_ABOUT, CREATED, DISCOVERED + LOCATED_AT, IS_DANGEROUS, HELPED, ATTACKED, HAS_ROLE, CONNECTS, IS_HONORABLE, TRADES_AT, LEADS, HAS_PROPERTY). `outgoing/incoming/neighbors/query/path` (BFS acotado). Nunca recorre todo el grafo para una consulta simple.
