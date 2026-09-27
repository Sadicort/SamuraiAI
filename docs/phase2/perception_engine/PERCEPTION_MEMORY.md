# Perception Memory

Clase `memory.PerceptionMemory`, `MemoryEntry`, `MemoryKind`.

Seis tipos con vida propia (ticks): `VISUAL` 600, `AUDITORY` 400, `ENVIRONMENTAL` 2400, `SOCIAL` 6000, `DANGER` 1800, `INTEREST` 1200. Las entradas **se desvanecen progresivamente** (la fuerza cae cuadráticamente con la edad, escalada por la importancia) y se olvidan por debajo de 0.05. Acotada por tipo: un lugar concurrido no la hace crecer sin límite.

- Cada entrada guarda `subject`, posición, importancia y **incertidumbre** (un sonido lejano se recuerda con margen).
- `lastKnown(subject)`, `strongest(kind)`, `forget(kind, key)`, `forget(subject)`.
- La importancia de un sonido mezcla prioridad de categoría (0.6) e intensidad (0.4) — antes una explosión lejana se olvidaba demasiado pronto.
- Alimenta `SEARCHING` (recuerda algo perdido u oído), la investigación y `AwarenessMap`.
- Solo en el hilo del servidor.
