# Narrativa generacional

**Código:** `FamilyEngine.tellStories(Person)` (mecanismo ya existente de la Fase 5; esta extensión no lo reescribe, la verifica y se apoya en ella).

## La regla que no se puede romper

Lo que vivió un antepasado (una guerra, una traición, un acto de heroísmo) **nunca** puede llegar a un descendiente como un recuerdo episódico propio (`p.importantMemories()`, ni una llamada a `social.experience(...)`). Solo puede llegar como **conocimiento enseñado** — algo que alguien le contó — a través de `social.learn(...)`, con un anciano o familiar vivo como quien se lo cuenta (`teller`). Esta es la especificación explícita de la Fase 5.5 y era una prueba **obligatoria** antes de dar por completa la extensión de identidad.

## Cómo se decide qué se cuenta y a quién

`tellStories(p)` se dispara cuando una persona llega a joven adulta (`LifeStage.YOUNG_ADULT`) o directamente a adulta si nace ya en esa etapa (línea 1109–1110 de `FamilyEngine`, dentro de `simulate`). Para cada recuerdo de la memoria familiar (`FamilyRecord.memory()`):

1. Se descarta si su significancia es menor que `storySignificance` (0.45 por defecto) — no todo se cuenta, solo lo memorable.
2. Se descarta si ocurrió **después** de que la persona naciera y ella no estuvo presente (`!e.persons().contains(p.id())`) y ya no es reciente (más de un año de juego) — es decir, si es algo que la propia persona vivió, esto no es su vía: eso ya llega por el camino normal de vivir el evento, no por narrativa generacional.
3. Se evita contar la misma historia dos veces a la misma persona (`storiesTold`, una clave por persona+evento+texto).
4. Quien cuenta la historia es el pariente vivo de más edad —un ancestro directo hasta 3 generaciones, o si no hay ninguno vivo, el miembro vivo de más edad de la familia— nunca un desconocido.

El recuerdo llega como `social.learn(persona, "family-story:...", texto, narrador)` — el mismo verbo que usa el motor cognitivo para cualquier otro conocimiento enseñado (una técnica, un secreto), nunca `social.experience(...)`, que es el verbo reservado para algo que la propia persona vivió.

## Por qué esto no duplica el motor cognitivo

`tellStories` no decide cómo se recuerda o se olvida el conocimiento una vez aprendido — eso sigue siendo enteramente responsabilidad de la capa cognitiva (Fase 3). `FamilyEngine` solo decide **qué contar y cuándo**, con datos que ya existían (`FamilyMemoryEntry`), y llama al mismo punto de entrada (`social.learn`) que cualquier otra enseñanza familiar.

## La prueba obligatoria

`FamilyIdentityExtensionTest.aFamilyStoryReachesADescendantAsKnowledgeNeverAsAMemoryOfSomethingBeforeTheyWereBorn`: registra una guerra real en la memoria de una familia, antes del nacimiento de un descendiente; cuando ese descendiente llega a la edad de escucharla, comprueba que:

- la historia aparece como un hecho de conocimiento aprendido (`social.learn`), con el pariente correcto como narrador;
- **no** aparece ni una sola llamada `social.experience(...)` a nombre de ese descendiente con esa guerra como causa;
- **no** aparece en `p.importantMemories()`.

Esta prueba confirma que el mecanismo ya existente de la Fase 5 cumplía la garantía sin cambios, y que ninguna de las nuevas llamadas a `social.experience(...)` que añade esta extensión (sucesión, epítetos, ceremonias — ver `FAMILY_CEREMONIES.md`) interfiere con ella.
