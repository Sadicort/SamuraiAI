# Threat Engine

Clase `awareness.ThreatEngine`, `ThreatSource`, `ThreatLevel` (`SAFE, WARNING, DANGER, CRITICAL`).

Convierte estímulos peligrosos en un panorama de amenaza: hostiles, daño recibido, explosiones, fuego, lava, caídas, jugadores agresivos, proyectiles. Cada fuente tiene puntuación (peso por `StimulusCategory`) que **se desvanece si deja de percibirse** (`threatDecayPerTick`=0.35). El nivel global es la peor fuente más una fracción de las demás. Umbrales: WARNING 15, DANGER 40, CRITICAL 70.

Reglas de diseño:
- **Informa, nunca reacciona.** Quien reacciona es el Brain (utilidad) y el scheduler (respuesta).
- Recibir daño es evidencia crítica (`beingHurtIsCriticalEvidence`: nivel CRITICAL y atención al daño).
- Las amenazas percibidas se **publican como peligro a Navigation** (`PerceptionService.collectThreats` → `DangerZone` con umbral `threatWarning`): el camino evita lo que el NPC teme (`perceivedThreatsBecomeDangerForNavigation`).

Evento: `ThreatDetectedEvent(key, level, score, category, label, x, y, z)`. Métrica: `threats`.
