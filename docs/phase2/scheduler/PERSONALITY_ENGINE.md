# Personality Engine

Clases `personality.{Trait, PersonalityTraits, PersonalityEngine, Temperament, ResponseKind, DriftCause}`.

## 10 rasgos (0–100)

`COURAGE, DISCIPLINE, SOCIABILITY, CURIOSITY, AGGRESSION, CAUTION, LOYALTY, PATIENCE, DILIGENCE, SPIRITUALITY`. **No hay personalidades en código**: cada estilo de vida aporta una base (`traits=` en su línea) y cada NPC se individualiza con un jitter determinista por identidad (`personalityJitter`=12).

## Qué hace la personalidad

- **Afinidad por rutina** (`affinity`, 0.25–2.5): coeficientes rasgo→rutina (p. ej. PRAYER ← espiritualidad; TRAINING ← disciplina, coraje, agresión). Intensidad `affinityStrength`.
- **Sesgo de respuesta** (`responseBias`): INVESTIGATE ← curiosidad y coraje − cautela; FLEE ← cautela − coraje; etc.
- **Temperamento** (`Temperament`, factores 1.0 = neutro): escala de visión y oído, curiosidad, ganancia de sospecha, miedo, atención, velocidad, aversión al peligro y **retardo de reacción** (ticks).
- **Espacio personal** (`personalSpace`) para la distancia social.
- **Deriva** (`drift`): experiencias (`FRIGHTENED, TRIUMPHED, SOCIALISED, TOILED, CONTEMPLATED, DISCOVERED, BETRAYED`) mueven rasgos y publican `PersonalityUpdatedEvent`. Se aplican al terminar rutinas (trabajar → diligencia, meditar → espiritualidad…) y por `/samuraiai scheduler experience`.

## Hacia otros motores (sin dependencia directa)

`SchedulerService.applyTemperaments` → `PerceptionService.setSenseProfile` (`SenseProfile`) y `NavigationService.setPreferenceAdjuster` (peso del peligro × `dangerAversion`). El retardo de reacción lo consume el propio scheduler (una respuesta situacional espera ese retardo).

**Pendiente:** `Temperament.speedFactor` se calcula y expone pero Navigation aún no lo consume (solo se elige RUN/WALK por contexto).

Pruebas: `PersonalityEnergyTest` (rasgos, temperamento, sesgos, individuos deterministas, deriva con límites).
