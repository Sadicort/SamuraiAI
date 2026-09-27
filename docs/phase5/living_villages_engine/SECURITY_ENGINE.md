# Security Engine

**Código:** `living/village/security/SecurityRuntime.java`, `SecurityState.java`; `VillageEngine.reportThreat/attackStarted/attackEnded`.

## Estados

`PEACE → ALERT → DANGER → ATTACK → RECOVERY → PEACE`.

- Amenaza acumulada 0..100 que **decae** `threatDecayPerHour` (12) por hora.
- Amenaza ≥ `alertThreshold` (25) → ALERT; ≥ `dangerThreshold` (60) → DANGER.
- Un ataque en curso (evento de mundo `ATTACK` abierto) → ATTACK; al terminar → RECOVERY; vuelve a PEACE cuando la amenaza sigue baja `recoveryMinutes` (360).
- `VillageSecurityChangedEvent(desde, hasta, motivo, amenaza)`.

## Fuentes de amenaza

| Fuente | Puntos |
| --- | --- |
| Percepción (`ThreatDetectedEvent` de un ciudadano, nivel ≥ DANGER) | `threatPerLevel` (15) × nivel (DANGER = 2 → 30, CRITICAL = 3 → 45), como mucho una vez cada 200 ticks por aldea |
| Combate de un ciudadano (`CombatStartedEvent`) | `combatThreat` (35), mismo límite |
| Bandidos en la región | 20 × severidad |
| Guerra en la región | 45 × severidad |

## Efecto

Guardias todos de servicio; civiles a casa (sesgo), mercado cerrado en ataque, sin reuniones sociales, menos visitantes; la misión de defensa la abre el evento de ataque. Ver prueba `threatRaisesAlertAndDangerAndDecays`.
