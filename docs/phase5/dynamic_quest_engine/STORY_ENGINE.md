# Story Engine — las etapas de una misión

**Código:** `QuestTemplate.StoryStage`, `Quest.stage/text`, `living/quest/dialogue/QuestDialogue.java`.

Cada misión cuenta una historia en cinco etapas, cada una con su texto de plantilla (con variables):

1. **PROLOGUE** — la oferta: qué pasa y quién pide ayuda.
2. **DEVELOPMENT** — al aceptar: qué hay que hacer.
3. **TWIST** — si la condición empeora ≥ `twistThreshold` (0,25) mientras está activa (o una caravana escoltada sufre una emboscada): los objetivos de entrega y combate pendientes crecen ×1,5 y se avisa a los jugadores («Giro en …»).
4. **ENDING** — cómo terminó.
5. **CONSEQUENCES** — qué cambió en el mundo.

`QuestDialogue` da la voz del que la ofrece según su emoción dominante (capa cognitiva) y su confianza en el jugador: seco si está enfadado, suplicante si tiene miedo, cálido si está contento; si no confía, guarda distancia o **se niega a pedir** (`refusal`). Las mismas frases se usan en el chat; el prompt de diálogo del NPC recibe las misiones que da (sección «TU MUNDO»: «Buscas quien te ayude con: …»).

Las misiones pueden **encadenarse** (`followUp`: el herrero al que ayudaste te forja algo, `forged_gift`) heredando variables y quien la da; `QuestChain` recuerda cada cadena desde su primera misión.
