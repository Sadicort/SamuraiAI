# Event Response Planner

Clase `response.EventResponsePlanner`. Dado lo que el NPC percibió (`Perceived`: amenaza 0–3, sospecha, objetivo de investigación, daño) o le avisaron (`Alarm`), y quién es (personalidad, rol, condición, si puede pelear), **propone** cómo responder. No actúa ni elige.

## Propuestas

| Situación | Candidatos (capa, fuente) |
| --- | --- |
| amenaza ≥ DANGER, herido o pánico | `FLEE` (EMERGENCY, SURVIVAL); si puede pelear, también `ASSIST` hacia la amenaza |
| algo que investigar (sonido) y sin gravedad | `INVESTIGATE` (SITUATIONAL, EVENT) hacia el objetivo, radio = incertidumbre + 1.5 |
| sospecha o amenaza leve sin objetivo | `WATCH` (SITUATIONAL) en el sitio |
| alarma del grupo | pelea: `ASSIST` (nivel 3 → EMERGENCY); civil nivel ≥ 2: `FLEE`; si no, `WATCH` |

Puntuación = base (`fleeBase`=90, `assistBase`=55, `investigateBase`=50, `watchBase`=35) × sesgo de personalidad × multiplicador de rol × urgencia × (fatiga ×0.6, estrés ×0.8).

**Huir:** en dirección contraria a la amenaza `fleeDistance`=20 bloques, o **a casa** si casa queda más lejos de la amenaza que el NPC.

**Retardo de reacción:** los candidatos situacionales esperan el retardo del temperamento; los de emergencia no. **Cooldown** (`responseCooldownTicks`=200) tras terminar una respuesta.

**Alarma:** si el NPC está en un grupo, ve amenaza/daño y su sesgo `RAISE_ALARM` ≥ 0.8, devuelve una `AlarmRequest` (nivel 1–3).

**Fin de una respuesta:** dura mientras su disparador se siga viendo, más `responseHoldTicks`=200 ("trigger gone").

Pruebas: patrulla→sonido→investigar→volver, emergencia sobre el sueño, ASSIST vs FLEE, alarma de grupo, retardo de reacción (unitarias y físicas).
