# Campaign Engine

**Código:** `living/quest/campaigns/Campaign.java`, `CampaignCatalog.java`; eventos `CampaignStartedEvent`, `CampaignAdvancedEvent`, `CampaignEndedEvent`.

Una campaña cuenta una historia en varias misiones y persiste entre reinicios (etapas, etapa actual, misión de cada etapa, estado, clave de la condición de origen).

| Campaña | Condición | Etapas |
| --- | --- | --- |
| La guerra de {región} (WAR) | WAR | reconocimiento → defender la aldea → tomar el paso |
| La ruta de {destino} (TRADE) | ROUTE_BLOCKED | reabrir el camino → escoltar la primera caravana |
| Los ritos de la luna (TEMPLE) | TEMPLE_RITUAL | el rito → la reliquia |
| El legado de los {familia} (FAMILY) | HEIRLOOM_LOST | recuperar la reliquia → restaurar el honor |
| Viejas rencillas (CLAN) | GRUDGE | resolver la disputa → restaurar el honor |

Al completarse una etapa se crea la siguiente con las variables heredadas; si una etapa falla, la campaña falla. `campaigns = false` en `samuraiai-quest.toml` las desactiva (las condiciones generan misiones sueltas).

Prueba: `campaignsAndChainsContinueTheStory`, `LivingWorldTest.aLastingDisputeBetweenNeighboursBecomesAFeudBetweenTheirFamilies`.
