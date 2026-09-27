# Sociedad

`society/SocietyEngine`, `Community` (aldea, templo, mercado, clan, facción, puesto de guardia): miembros con **rango** (`AccessLevel`), líder, cultura, **conocimiento colectivo** (un `KnowledgeRuntime` propio), historia, estado de tradiciones, reputación pública, apoyo por hecho.
- **Adopción colectiva**: un hecho pasa a la comunidad cuando ≥ max(`collectiveMin`, `collectiveFraction`·miembros) de sus miembros lo creen.
- El adaptador une a cada NPC a la comunidad más cercana a su hogar (o crea una aldea, `autoVillage`).
`/samuraiai society communities|create|join|leave|history|timeline|rumors|legends|cultures`.
