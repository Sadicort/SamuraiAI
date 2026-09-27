# Olvido

`forgetting/ForgettingEngine` + `ForgettingCurve` (exponencial). Semivida = `halfLifeTicks · escala(importancia) · (1+emociónRetención·pesoEmocional) · (1+usoRetención·accesos) · (1+log₂ repeticiones·0,25) · (trauma ? traumaRetención : 1) · (1+personalityRetention·lean(DISCIPLINE))`.

- Fuerza < `fadingThreshold` → FADING (se recupera si se rememora); < `forgetThreshold` → olvidado (salvo CRITICAL+ y protegidos).
- **Nunca instantáneo**; el decaimiento se calcula perezosamente desde `lastDecay`.
- **Protegidos** (`ProtectionPolicy`): importancia ≥ `protectFromImportance` (CRITICAL), traumáticos, tags `oath/identity/vow/trauma`, kinds configurados.
- Capacidad: `maxMemories`; al superarla se olvidan los de menor valor (`fuerza·(0,3+peso)`).
- El olvido administrativo explícito es la única vía para quitar un protegido (`/samuraiai memory forget`).
