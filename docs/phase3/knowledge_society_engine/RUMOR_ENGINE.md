# Rumores

`RumorRecord`: id, **origen** (NPC + id de recuerdo + traza; sin origen lanza `IllegalArgumentException`), reclamo (`RumorClaim`: sujeto, predicado, objeto, etiqueta de reputación, magnitud, lugar), estado (UNKNOWN, ACTIVE, CONFIRMED, FALSE, FORGOTTEN), fuerza, **saltos** (de/a/cuándo/credibilidad/transformado), **transformaciones**, portadores.
Ciclo: creación (testigo de un suceso público) → propagación (`gossip` + cola) → transformación (determinista por id y salto, acotada por `distortionMax`, nunca cambia el sujeto; cada salto atenúa por `rumorHopDecay`; máx `maxHops`) → confirmación/rechazo (`resolveRumor`: los portadores reciben evidencia independiente o contradicción) → olvido (sin repetición). Para el oyente el rumor es **RUMOR**, no verdad.
