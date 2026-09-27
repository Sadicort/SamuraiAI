# Quest History Engine

**Código:** `living/quest/memory/QuestHistory.java`.

- Cada misión aceptada, completada, fallida, abandonada o resuelta por el mundo, con el camino, las decisiones y el final.
- Por jugador (lo que el mundo recuerda que hiciste; hasta `historyPerPlayer` 200) y del mundo (hasta `historyWorld` 1000), más totales.
- `/samuraiai living quest history` (tu historia).
- Persistida con las misiones (`quest/quests.json`).

Las misiones terminadas se borran del registro vivo a los 30 días; la historia permanece.
