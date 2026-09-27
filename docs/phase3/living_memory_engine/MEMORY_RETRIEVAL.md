# Recuperación

- `RetrievalQuery`: persona, lugar (celda), zona, emoción, rango de días, importancia mínima, categoría, kind, tipo, tags, evento, fuerza mínima, límite, modo.
- **EXACT** exige todos los criterios (parte del conjunto más pequeño); **FUZZY** exige la fracción `fuzzyMinMatch`; **SIMILAR** ordena por similitud (`SimilarityEngine`, hoy estructural: kind, actor, lugar, emoción, Jaccard de tags; costura para embeddings futuros).
- Puntuación = ajuste · (0,35+0,65·fuerza) · (0,5+0,5·importancia) · recencia. Recordar **ensaya** el recuerdo (sube fuerza, cuenta acceso).
- **Contexto** (`ContextRetrieval`): "¿qué recuerdos son relevantes ahora?" desde personas cercanas, lugar, zona, emoción y tags del objetivo, sin recorrer el historial; resultado cacheado unos ticks.
