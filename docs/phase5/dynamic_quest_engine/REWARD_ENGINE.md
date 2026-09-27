# Reward Engine

**Código:** `living/quest/rewards/RewardSpec.java`; `QuestEngine.giveRewards`.

| Recompensa | Cómo se paga |
| --- | --- |
| COINS | del **tesoro** del asentamiento (`economy.reward`, nunca más de lo que tiene) |
| ITEMS | del **almacén** del asentamiento con su procedencia → objetos reales al jugador |
| REPUTATION | posición del jugador en la comunidad de la aldea, contexto `village/temple/clan/merchants/guards/monks` |
| KNOWLEDGE | lo que cuenta el dador (un lugar, una historia), por chat |
| RELATIONSHIP | experiencia `HELPED_ME` del dador con el jugador |
| TITLE | preparado (sin efecto aún) |

Factor total = factor del camino × ánimo del dador (×1,1 contento, ×0,9 enfadado) × `rewardScale` (1,0). Cada recompensa publica `QuestRewardGivenEvent` y queda en la misión (`given`).

Prueba: `deliveringCompletesTheQuestPaysFromTheTreasuryAndChangesTheWorld`.
