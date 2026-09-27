# Plantillas de misión

**Código:** `living/quest/templates/QuestTemplate.java`, `QuestTemplates.java`, `Expr.java`.

Una plantilla: id, categoría (`STORY, ECONOMY, EXPLORATION, INVESTIGATION, ESCORT, COMBAT, RELIGION, TRAINING, MYSTERY, DIPLOMACY`), condiciones que la disparan, título, texto por etapa de la historia, objetivos, caminos, recompensas, consecuencias, días de oferta y de plazo, estaciones y culturas, oficios de quien la da, peso, seguimiento (cadena), si se puede fusionar y confianza mínima del que la da en el jugador.

## Integradas (21)

| Id | Disparador | Título |
| --- | --- | --- |
| village_food_shortage | SCARCITY | Hambre en {settlement} |
| workshop_needs_material | WORKSHOP_STARVED | {giverProfessionName} sin {resourceName} |
| forged_gift | CUSTOM (seguimiento) | El regalo de {giver} |
| guards_need_help | GUARD_SHORTAGE, BANDITS | La guardia de {settlement} pide ayuda |
| defend_village | ATTACK | ¡Defender {settlement}! |
| fire_aid | FIRE | Reconstruir tras el fuego |
| housing | HOUSING_SHORTAGE | Una casa más para {settlement} |
| festival_preparation | FESTIVAL_SOON | Preparativos para {festival} |
| temple_ritual | TEMPLE_RITUAL | El rito de la luna llena |
| temple_relic | HEIRLOOM_LOST, EXPLORATION | La reliquia perdida |
| escort_caravan | LOST_CARAVAN, BANDITS | Escoltar la caravana hacia {destinationName} |
| open_trade_route | ROUTE_BLOCKED, BANDITS | Reabrir el camino a {destinationName} |
| war_reconnaissance | WAR | Reconocimiento: {regionName} |
| capture_bridge | WAR | Tomar el paso de {regionName} |
| gratitude_gift | GRATITUDE | {giver} quiere darte las gracias |
| resolve_dispute | DISPUTE, GRUDGE | Una disputa en {settlement} |
| rescue_relative | MISSING_PERSON | ¿Dónde está {personName}? |
| recover_heirloom | HEIRLOOM_LOST | {heirloomName} de la familia {familyName} |
| restore_honor | FAMILY_DISHONOR | El honor de la familia {familyName} |
| train_disciple | MENTOR_WANTED | {master} busca un discípulo |
| explore_region | EXPLORATION | Tierras desconocidas: {regionName} |

## Plantillas por configuración

Clave `templates` de `samuraiai-quest.toml`, líneas como `honey;category=ECONOMY;when=SCARCITY;title=Miel para {settlement};text=...;objective=DELIVER:{resource}:{quantity}:settlement;reward=COINS:{value};reputation=VILLAGE:0.05`. Un id existente se sustituye; `disabled=true` elimina una.

## Expresiones (`Expr`)

Texto con `{variable}` (una variable desconocida se queda visible para que se note) y cantidades: número, variable, o variable × / + número (`{quantity}*1.5`, `{severity}*40+10`; una variable desconocida vale 0).
