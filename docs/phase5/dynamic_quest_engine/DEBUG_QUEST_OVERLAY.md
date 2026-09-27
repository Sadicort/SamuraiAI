# Depuración de misiones

**Código:** `living/quest/debug/QuestInspector.java`; comandos en `LivingCommand`.

| Comando | Permiso | Qué hace |
| --- | --- | --- |
| `/samuraiai living quest list` | todos | tus misiones activas y las ofrecidas en la aldea donde estás (id de 8 caracteres) |
| `/samuraiai living quest info <id>` | todos | origen, causa, plantilla, variables, objetivos, caminos, decisiones, consecuencias, dador, condición |
| `/samuraiai living quest accept <id>` | todos | aceptar (puede negarse si el dador no confía en ti) |
| `/samuraiai living quest paths <id>` | todos | caminos disponibles para ti |
| `/samuraiai living quest choose <id> <camino>` | todos | elegir camino |
| `/samuraiai living quest abandon <id>` | todos | abandonar (consecuencias de fracaso) |
| `/samuraiai living quest deliver <recurso> <cantidad>` | todos | entregar objetos en la aldea |
| `/samuraiai living quest history` | todos | tu historia |
| `/samuraiai living quest all` | op | todas las misiones |
| `/samuraiai living quest campaigns` | op | campañas |

Al hablar con un NPC que busca ayuda, el jugador recibe «X necesita ayuda: … (/samuraiai living quest accept …)» (como mucho cada 5 minutos por NPC). `debugLogging` en `samuraiai-quest.toml` registra los eventos de misión.
