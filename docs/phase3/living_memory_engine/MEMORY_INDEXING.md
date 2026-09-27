# Índices

`indexing/MemoryIndex`: por entidad, celda de espacio (`cellSize`), zona, tag, evento, día, emoción, importancia, categoría, kind y tipo. `reindex(record, cambio)` mantiene la consistencia cuando un recuerdo cambia de claves.

Coste: una consulta lee conjuntos pequeños; la prueba con 3 000 recuerdos mide < 2 ms por consulta. **Cuidado**: al reindexar se itera una copia del conjunto (bug real hallado con los tests: `ConcurrentModificationException` en la reinterpretación).
