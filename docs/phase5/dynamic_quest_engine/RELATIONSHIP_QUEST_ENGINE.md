# Misiones de relación

- **Confianza para pedir:** una plantilla puede exigir confianza mínima del que la da en el jugador (`minTrust`, relaciones cognitivas 0..100); sin ella, el NPC no pide (prueba `aGiverWhoDoesNotTrustThePlayerDoesNotAsk`). Los **caminos** también pueden exigir confianza y posición (`BRANCHING_ENGINE.md`).
- **Gratitud:** un NPC agradecido con un jugador ofrece «{giver} quiere darte las gracias» (conocimiento, relación y monedas); `trust(55)`.
- **Disputas:** dos vecinos con rivalidad fuerte → «Una disputa en {aldea}» (hablar con los dos; caminos diplomático u honorable). Si dura entre dos familias → las familias se vuelven rivales → campaña **«Viejas rencillas»** (`resolve_dispute` → `restore_honor`).
- **Recompensa de relación:** el que la da vive la experiencia `HELPED_ME` con el jugador (memoria, emoción, relación, a través del catálogo cognitivo); una consecuencia de relación negativa es la experiencia `BETRAYED`.
- **Hablar** con un NPC cumple objetivos `TALK` (`LivingService.conversation` tras una respuesta de diálogo): por id, por oficio (`priest` acepta monjes), `giver` o cualquier `citizen`.
