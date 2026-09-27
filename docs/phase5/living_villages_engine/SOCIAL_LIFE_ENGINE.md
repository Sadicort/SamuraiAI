# Social Life Engine

**Código:** `living/village/social/SocialLifeEngine.java`.

- **Actividad social** de la aldea: gente cuyo plan la tiene socializando ahora, escalada por la estación (`social` del perfil de estación), los festivales y la seguridad.
- **Quién se junta con quién:** vecinos de casa, compañeros del mismo oficio, guardias con guardias, monjes con monjes. Si el grupo de un ciudadano está socializando, él recibe una pequeña atracción `SOCIAL` (`neighbourSocialBias`, 10 puntos, hasta ×3).
- `safeToGather(aldea)`: sin reuniones en peligro o ataque.

Qué se dicen y qué sienten al reunirse sigue siendo de la capa cognitiva (cotilleo, contagio emocional): este motor solo decide **que se encuentran**.
