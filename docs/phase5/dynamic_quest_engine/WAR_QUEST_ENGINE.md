# Misiones de guerra

No hay un «Battlefield Engine»: la guerra entra como eventos de mundo `WAR`, `ATTACK` y `BANDITS` (`../living_world_engine/WORLD_EVENT_ENGINE.md`).

- **Campaña «La guerra de {región}»**: `war_reconnaissance` (investigar el centro de la región) → `defend_village` → `capture_bridge` (tomar el paso).
- **¡Defender {aldea}!** (`defend_village`): objetivo `DEFEND` — estar en la aldea mientras dura el ataque (el adaptador anota a los jugadores presentes como defensores). Al terminar el evento: los defensores ganan; si nadie estuvo, la misión falla y la aldea sufre saqueo.
- **Combate:** `killed(jugador, tipo, hostil, lugar)` cuenta los enemigos que matan los jugadores (`LivingDeathEvent` con un jugador como asesino; «hostil» = entidad `Enemy`).
- Terminar la misión puede **resolver el evento** (`RESOLVE_EVENT`: los bandidos se van) o, si falla, **abrir otro** (`SPAWN_EVENT`: saqueadores).
- La guerra bloquea caminos, corta rutas y abre misiones de ruta y de escolta (prueba `LivingWorldTest.warCutsTradeAndOpensQuests`).
