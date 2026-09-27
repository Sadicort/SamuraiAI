# Reputation Engine (misiones)

**Código:** `living/quest/reputation/ReputationContext.java`; puerto `QuestPorts.Social.standing/adjustStanding` → `Outside` → `SocietyEngine.adjustStanding`.

**No hay un segundo sistema de reputación.** Los contextos (`VILLAGE, TEMPLE, CLAN, MERCHANTS, GUARDS, MONKS`) son etiquetas de la posición (`standing`) que las comunidades de Knowledge ya guardan por persona. Las misiones:

- **premian** posición en un contexto (recompensa `REPUTATION`, consecuencia `REPUTATION`);
- **exigen** posición para algunos caminos (`minStanding`);
- la reputación **familiar** es otra cosa, propiedad de Family (`../family_lineage_legacy_engine/REPUTATION_HERITAGE.md`), y la consecuencia `FAMILY_HONOR` la cambia.
