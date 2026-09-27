# Comandos de SamuraiAI

Todos cuelgan de `/samuraiai`. Nivel de permiso: **admin** = operador nivel 2 o más; sin marca = cualquier jugador. `<npc>` es el nombre de un NPC activo (se autocompleta con Tab). Los comandos marcados *(jugador)* solo funcionan si los ejecuta un jugador, no la consola.

## Generales

| Comando | Permiso | Qué hace |
| --- | --- | --- |
| `/samuraiai spawn <tipo> [nombre]` | admin, jugador | Crea un NPC del tipo indicado (`samurai`, `guard`, `merchant`…) en tu posición y mirando hacia donde miras. Si no das nombre, se genera uno único. |
| `/samuraiai types` | todos | Lista los tipos de NPC que se pueden crear. |
| `/samuraiai list` | todos | Lista los NPC activos. |
| `/samuraiai remove <nombre>` | admin | Elimina un NPC y su avatar. |
| `/samuraiai removeall` | admin | Elimina todos los NPC activos y dice cuántos eran. |
| `/samuraiai talk <nombre> <mensaje>` | todos, jugador | Habla con un NPC: el mensaje va al modelo (Ollama) y la respuesta llega por chat. |
| `/samuraiai status` | admin | Resumen: NPC activos, modelo de Ollama, y peticiones OK/fallidas. |

## Navegación — `/samuraiai nav …` (admin)

| Comando | Qué hace |
| --- | --- |
| `nav status` | Métricas del motor: rutas creadas, recálculos, atascos, coste por tick… |
| `nav inspect <npc>` | Estado de la sesión de navegación del NPC: ruta, nodo actual, último evento, fallos. |
| `nav goto <npc> <x y z>` | Manda al NPC a esa posición por el motor de navegación (puertas, obstáculos, recuperación). |
| `nav cancel <npc>` | Cancela su viaje y lo detiene. |
| `nav terrain <npc>` | Muestra el tipo de terreno bajo el NPC y el coste que le asigna la navegación. |
| `nav danger <x y z> <radio> <puntuación> <segundos>` | Crea una zona de peligro temporal que las rutas intentan evitar (útil para probar replanificación). |
| `nav debug` *(jugador)* | Activa/desactiva el overlay de partículas: ruta, nodos, puertas y peligro. |
| `nav metrics reset` | Pone a cero las métricas. |

## Percepción — `/samuraiai perception …` (admin)

| Comando | Qué hace |
| --- | --- |
| `perception status` | Métricas: pases, rayos, sonidos oídos, amenazas, fallos de sensor… |
| `perception inspect <npc>` | Qué percibe el NPC: conciencia, atención y foco, amenaza, sospecha, objetivos vistos, sonidos, interés, objetivo de investigación, memoria y estado de cada sensor. |
| `perception sound <x y z> <categoría> <volumen 0-1>` | Emite un sonido de prueba (`DOOR`, `EXPLOSION`, `BLOCK_BREAK`…) para ver cómo reaccionan los NPC cercanos. |
| `perception debug` *(jugador)* | Overlay: amarillo campo visual, verde visto, naranja tapado, gris memoria, cian sonido, rojo amenaza, dorado investigación. |
| `perception metrics reset` | Pone a cero las métricas. |

## Scheduler (agenda y comportamiento) — `/samuraiai scheduler …` (admin)

| Comando | Qué hace |
| --- | --- |
| `scheduler status` | Periodo del día, eventos del calendario activos, población por bucket, coste y contadores. |
| `scheduler inspect <npc>` | Cómo decide el NPC: estilo de vida, rasgos, energía/fatiga/estrés, rutina actual y **por qué**, pila de interrupciones y los mejores candidatos con su puntuación. |
| `scheduler zones` | Lista las zonas (tipo, radio, ocupación, propietario, alerta). |
| `scheduler zone add <id> <tipo> <radio> [capacidad]` | Crea una zona en tu posición. Tipos: `HOME, WORK, MARKET, TEMPLE, TRAINING, GUARD_POST, DINING, PLAZA, PATROL_ROUTE, REST_AREA`. Se guarda con el mundo. |
| `scheduler zone remove <id>` | Elimina una zona. |
| `scheduler zone claim <id> <npc o grupo>` | Da la zona a un NPC o grupo: solo ellos la usan mientras haya alternativa. |
| `scheduler groups` | Lista los grupos (líder, formación, alarma). |
| `scheduler group create <id> <tipo>` | Crea un grupo fijo (`PATROL, GUARD, MERCHANT, VILLAGE, SQUAD`) que la formación automática no toca. |
| `scheduler group add <id> <npc>` | Añade un NPC al grupo (si no está lleno). |
| `scheduler group disband <id>` | Disuelve el grupo. |
| `scheduler group formation <id> <forma>` | Fuerza la formación: `LINE, COLUMN, CIRCLE, DIAMOND, ESCORT, TRIANGLE`. |
| `scheduler experience <npc> <causa> <cantidad>` | Aplica una experiencia que cambia la personalidad (`FRIGHTENED, TRIUMPHED, SOCIALISED, TOILED, CONTEMPLATED, DISCOVERED, BETRAYED`). |
| `scheduler debug` *(jugador)* | Overlay: anillos de zona (rojo = alerta), dorado destino de la rutina, blanco líder, cian puesto en formación. |
| `scheduler metrics reset` | Pone a cero las métricas. |

## Capa cognitiva (Fase 3) — `/samuraiai mind|memory|relationship|emotion|knowledge|society …` (admin)

| Comando | Qué hace |
| --- | --- |
| `mind status` | Resumen de todos los motores: recuerdos, relaciones, emociones, conocimiento, sociedad, persistencia y costes. |
| `mind inspect <npc>` | Inspector cognitivo unificado de un NPC (personalidad, memoria, conocimiento, relaciones, emociones, comunidades, rastro). |
| `mind trace <npc>` | Últimos pasos de las experiencias del NPC (mundo→memoria→emoción→relación→conocimiento). |
| `mind experience <npc> <KIND> [actor]` | Hace vivir una experiencia al NPC (p. ej. `HELPED_ME`, `BETRAYED`) y muestra sus consecuencias. |
| `mind why <npc> <trust\|respect\|honor\|affinity\|rivalry\|fear\|loyalty> <persona>` / `emotion <KIND>` / `knowledge <nombre>` | Explica **por qué** el NPC siente, cree o piensa eso. |
| `mind save` / `mind debug` | Fuerza el guardado / activa el overlay (ánimo sobre el NPC y lugares conocidos). |
| `memory status\|list\|search\|show\|forget\|protect\|graph\|index\|export\|metrics reset` | Explora y administra los recuerdos (`export` escribe un snapshot JSON). |
| `relationship status\|list\|show\|timeline\|reputation\|graph\|promise\|promises\|fulfill\|break` | Relaciones direccionales, reputación, promesas y consultas del grafo social. |
| `emotion status\|show\|history\|tone\|trigger\|recover\|regulate\|metrics reset` | Estado emocional, traumas, tono de habla y disparo manual. |
| `knowledge status\|list\|places\|ask\|graph\|teach\|metrics reset` | Lo que el NPC cree saber, y lecciones entre NPC. |
| `society status\|communities\|create\|join\|leave\|history\|timeline\|rumors\|legends\|cultures` | Comunidades, cultura, historia, rumores y leyendas. |

Configuración: `samuraiai-cognition.toml`, `samuraiai-memory.toml`, `samuraiai-relationship.toml`, `samuraiai-emotion.toml`, `samuraiai-knowledge.toml`. Detalle: `docs/phase3/`.

## Mundo vivo (Fase 5) — `/samuraiai living …`

`<aldea>` es el nombre de una aldea con `_` en lugar de espacios (se autocompleta). `<id>` de misión: los 8 primeros caracteres. `<recurso>`: `rice`, `wheat`, `fish`, `meat`, `meal`, `wood`, `coal`, `iron`, `stone`, `cloth`, `leather`, `bamboo`, `herbs`, `clay`, `tools`, `lumber`, `gold` (los que tienen objeto de Minecraft).

### Para todos

| Comando | Qué hace |
| --- | --- |
| `living status` | Fecha de Deiliora, estación, clima, temperatura y luna donde estás; recuento del mundo. |
| `living hud` *(jugador)* | Activa/desactiva el calendario en la barra de acción. |
| `living calendar [weather\|festivals\|agriculture\|timeline]` | Calendario, clima de tu región, festivales, cultivos, cronología. |
| `living village` *(jugador)* / `living village list` / `living village info <aldea>` | La aldea donde estás / todas / una. |
| `living economy` *(jugador)* / `living economy prices <aldea>` | Precios y por qué. |
| `living trade sell <recurso> <cantidad>` *(jugador)* | Vende objetos a la aldea (paga el tesoro, según la confianza de sus mercaderes en ti). |
| `living trade buy <recurso> <cantidad>` *(jugador)* | Compra del almacén de la aldea. |
| `living coins` *(jugador)* | Tus monedas. |
| `living contract list` / `living contract deliver <id> <cantidad>` *(jugador)* | Contratos de entrega de la aldea (surgen por escasez) y entregarlos. |
| `living quest list` *(jugador)* | Tus misiones activas y las que se ofrecen aquí. |
| `living quest info <id>` | Causa, objetivos, caminos, consecuencias. |
| `living quest accept <id>` / `abandon <id>` *(jugador)* | Aceptar (el dador puede no confiar en ti) / abandonar. |
| `living quest paths <id>` / `choose <id> <camino>` *(jugador)* | Caminos (pacífico, violento, sigiloso, diplomático, espiritual, honorable). |
| `living quest deliver <recurso> <cantidad>` *(jugador)* | Entregar objetos en la aldea para tus misiones. |
| `living quest history` *(jugador)* | Lo que el mundo recuerda que hiciste. |

Además: hablar con un NPC cumple objetivos de conversación y te cuenta si necesita ayuda; llegar a un lugar, **agacharte quieto** (meditar), matar hostiles y estar en una aldea atacada cuentan para las misiones.

### Admin

| Comando | Qué hace |
| --- | --- |
| `living metrics` | Coste y volumen del hub, interacciones físicas y los seis motores. |
| `living save` | Guarda todo el mundo vivo ahora. |
| `living debug` *(jugador)* | Overlay: bordes de aldea por seguridad, edificios por estado, camas, caravanas. |
| `living calendar advance <días>` | Adelanta el tiempo de Deiliora (nunca atrás). |
| `living calendar weather <tipo> <horas>` | Fuerza el clima de tu región. |
| `living world [region\|settlements\|roads\|events [archive]]` | Mundo, tu región, asentamientos, caminos, eventos. |
| `living world event <tipo> <severidad>` | Programa un evento de mundo (`fire`, `attack`, `bandits`, `war`, `flood`, `market`…) en tu región. |
| `living world resolve <evento> si\|no` | Termina un evento antes de tiempo. |
| `living village citizens\|buildings\|visitors <aldea>` | Detalle de una aldea. |
| `living village citizen <npc>` | Ciudadano, plan del día y sesgo de rutinas con razones. |
| `living village found "<nombre>" [radio] [plan]` *(jugador)* | Funda una aldea aquí (con plano de distritos si `plan`). |
| `living village building <tipo> [radio]` *(jugador)* | Registra un edificio aquí. |
| `living economy settlement\|ledger <aldea>`, `provenance <aldea> <recurso>` | Tesoro, almacén, libro, procedencia de las existencias. |
| `living economy merchants\|caravans\|routes\|contracts` | Comercio. |
| `living quest all` / `living quest campaigns` | Todas las misiones / campañas. |
| `living family person\|info\|tree <npc>`, `lineage <nombre>`, `heirloom show <nombre>`, `audit` | Personas, familias, árbol, escuelas, reliquias, validación. |
| `living family birth <npc> <otro> <nombre>` | Registra un nacimiento. |
| `living family partners <npc> <otro>` | Registra una pareja (validada). |
| `living family mentor <maestro> <discípulo> <tipo>` | Empieza un aprendizaje (`craft`, `samurai`, `religious`…). |
| `living family technique <npc> <clave> <nombre>` / `teach <clave> <de> <a>` | Técnicas. |
| `living family lineage create <tipo> <fundador> <nombre>` | Funda un linaje o escuela. |
| `living family heirloom create <npc> <tipo> <nombre>` / `give <reliquia> <npc>` / `lost\|found <reliquia>` | Reliquias. |
| `living family designate <npc>` / `succession <npc>` / `branch <npc>` / `suggest <npc>` / `legacy <npc>` | Heredero, sucesión, rama, oficios sugeridos, legado. |
| `living family identity <npc>` | Nombre en partes, casa, clan, linaje, título, epíteto, cultura y orden. |
| `living family clan list` / `clan info <nombre>` | Todos los clanes / detalle de uno. |
| `living family clan create <npc-fundador> <nombre>` / `clan join <clan> <npc>` / `clan leave <clan> <npc>` | Funda, une o retira una familia de un clan. |
| `living family names <cultura> <cantidad>` | Tanda de nombres de muestra de una cultura (`yamato\|ashen\|western_march\|old_flame\|hollow`), sin persistir nada. |

## Notas

- Los overlays (`debug`) son partículas visibles solo para quien los activa; se apagan repitiendo el comando.
- Los tres motores tienen archivo de configuración propio (`samuraiai-navigation.toml`, `samuraiai-perception.toml`, `samuraiai-scheduler.toml`).
- Más detalle: `docs/phase2/navigation_engine/DEBUG_NAVIGATION.md`, `perception_engine/DEBUG_PERCEPTION.md`, `scheduler/DEBUG_SCHEDULER.md`.
