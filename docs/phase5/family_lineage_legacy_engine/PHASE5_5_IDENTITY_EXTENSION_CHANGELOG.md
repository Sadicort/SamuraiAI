# Changelog — Extensión de identidad de la Fase 5.5 (nombres, clanes, ancianos y legado generacional)

Esta es una **extensión** del motor de Familia, Linaje y Legado ya completo (`PHASE5_5_CHANGELOG.md`), no un motor nuevo: nada de lo descrito aquí sustituye a lo que ya existía; todo se añade sobre ello o lo completa.

## Añadido

- **Nombres de fantasía oscura**: cuatro culturas de nombre nuevas (Ceniza del Norte, Marca Occidental, Vieja Llama, Los Huecos) junto a la yamato existente, cada una con sus propios nombres de pila, apellidos curados y raíces de composición, orden de lectura propio (apellido primero en yamato, nombre primero en las otras cuatro) y un validador de calidad que se aplica a todas por igual. Reparto configurable por región (`nameCultureWeights`). Ver `DARK_FANTASY_NAME_ENGINE.md`, `CULTURAL_NAMING_PROFILES.md`, `SURNAME_GENERATION.md`.
- **Casas**: una familia con suficiente historia e importancia pasa a llevar un título de casa («Casa Ashborne»), concedido una vez y para siempre, en la palabra propia de su cultura de nombre. Ver `HOUSE_NAMING.md`.
- **Clanes**: una capa social sobre varias familias (`ClanRecord`), con líder elegido por mérito de familia (no hereditario), reputación y honor propios, historia, estado (`FORMING/ACTIVE/DECLINING/DISPERSED/EXTINCT/HISTORICAL`) y relaciones con otros clanes. Se fundan, se unen a, se abandonan y heredan por rama; un clan disperso se recuerda, nunca se borra. Ver `CLAN_ENGINE.md`.
- **Epítetos**: títulos ganados («Aldren Ashborne, el Honorable») siempre por una causa real y rastreable — jefatura, maestría, fin de aprendizaje, honor o deshonor acumulado, fundación de un clan — nunca al azar, siempre en el estilo de la crónica castellana independientemente de la cultura del nombre de la persona. Reliquias suficientemente importantes también ganan un nombre propio. Ver `EPITHET_ENGINE.md`.
- **Integración con la creación de NPCs**: un nombre que el sistema le puso a un NPC de repuesto («Merchant_3») se sustituye por un nombre real de la cultura de su familia la primera vez que se le da familia; un nombre que alguien eligió jamás se toca, y ningún nombre se regenera después de asignado. Ver `NPC_CREATOR_NAMING_INTEGRATION.md`.
- **Conocimiento de los ancianos**: quién es el anciano de una aldea (el residente vivo de más edad) y de qué puede hablar con propiedad (guerras, oficios y técnicas de su propia familia y linaje), sin un sistema de edad o de conocimiento nuevos. Ver `ELDER_KNOWLEDGE_INTEGRATION.md`.
- **Narrativa generacional verificada**: se confirmó con una prueba obligatoria que lo que vivió un antepasado solo puede llegar a un descendiente como conocimiento enseñado, nunca como su propio recuerdo — el mecanismo ya existía en la Fase 5 (`FamilyEngine.tellStories`) y se comprobó explícitamente que las nuevas ceremonias no lo rompen. Ver `GENERATIONAL_STORYTELLING.md`.
- **Ceremonias familiares**: sucesión, transición de maestro, fin de aprendizaje, entrega de reliquia, epíteto ganado y fundación de clan avisan ahora a la capa cognitiva existente (`social.experience`) con verbos reales del catálogo, para que emoción, relación, memoria y reputación reaccionen con la maquinaria ya construida en la Fase 3. Ver `FAMILY_CEREMONIES.md`.
- **Ganchos de misión familiares**: dos condiciones de misión que ya existían en el catálogo desde la Fase 5 pero nunca se disparaban (`HEIRLOOM_LOST`, `FAMILY_DISHONOR`) ahora se reportan de verdad al Quest Generator existente — una reliquia perdida se convierte en una misión de recuperación, y el honor de una familia que cae bajo un umbral se convierte en una misión para restaurarlo. Ningún motor de misiones nuevo; ver la sección correspondiente en `FAMILY_CEREMONIES.md`.
- **Depurador**: `family identity <npc>` (nombre en partes, casa, clan, linaje, epíteto, cultura, semilla), `family clan list/info/create/join/leave`, y `family names <cultura> <cantidad>` para generar tandas de nombres de muestra sin tocar ninguna familia real. Ver `FAMILY_NAME_DEBUGGER.md`.
- 9 eventos nuevos (`HouseTitleGrantedEvent`, `EpithetGrantedEvent`, `ArtifactNamedEvent`, `ClanCreatedEvent`, `ClanJoinedEvent`, `ClanLeftEvent`, `ClanLeaderChangedEvent`, `ClanDisbandedEvent`, `HeirloomLostEvent`); persistencia de todo lo anterior en los ficheros ya existentes de `FamilyStorage` (sin fichero nuevo); 9 pruebas unitarias nuevas de nombres (`NamingEngineTest`) y 9 de identidad/clanes/ancianos/narrativa/persistencia (`FamilyIdentityExtensionTest`), sin ninguna regresión en las 502 pruebas del proyecto ni en los 26 GameTests de cada perfil de compilación.

## Corregido

- **La sustitución de nombres genéricos nunca se disparaba en el juego real.** `LivingReactions.citizenJoined` llamaba a la sobrecarga de `FamilyEngine.adopt` sin tipo de NPC, así que `NamingEngine.isGeneric` siempre veía un tipo vacío y devolvía `false` — el código y las pruebas unitarias de la integración eran correctos por su cuenta, pero estaban desconectados del punto de entrada real. Se corrigió pasando `Citizen.npcType()` a la llamada. Ver `NPC_CREATOR_NAMING_INTEGRATION.md`.

## Decisiones de diseño

- **Yamato es una cultura más, no la única**: para no contradecir el calendario, los festivales y los oficios japoneses ya existentes, ni obligar a toda familia nueva a sonar igual, el reparto por defecto (`yamato:35, ashen:20, western_march:20, old_flame:15, hollow:10`) deja a yamato como la cultura individual más grande pero a las cuatro nuevas juntas con la mayoría.
- **La palabra de casa es «Casa» en las cinco culturas** por ahora — una simplificación consciente; el campo existe por cultura para poder diferenciarlas en el futuro sin tocar el motor.
- **Los clanes viven dentro de `FamilyEngine`**, no en un motor propio: son una capa social sobre las familias, no una genealogía paralela.
- **Reintento con respaldo, no un validador permisivo**: `SurnameGenerator` intenta un compuesto validado hasta 5 veces antes de caer a una palabra curada garantizada, en vez de relajar el validador para que todo compuesto pase.

## Límites (léase antes de asumir algo que no está)

- Todas las culturas usan la palabra «Casa» para una casa importante; no hay variación léxica por cultura todavía.
- `SurnameOrigin.HONORIFIC/SYMBOLIC/OCCUPATIONAL/HISTORICAL` son categorías reservadas sin generador propio; `TOPONYMIC` y `ANCESTRAL` tienen código y prueba pero ningún llamador automático desde `FamilyEngine` — una rama sigue llevando el apellido de su familia madre.
- Las relaciones entre clanes (aliado/rival) se pueden guardar y leer, pero nada las escribe automáticamente; no hay guerra ni alianza de clanes emergente.
- No hay aniversario de fundación de familia ni ceremonia de juramento — ninguna de las dos tenía un disparador natural en el código existente, y no se improvisó uno.
- No hay ganchos de misión específicos de clan (disputa de clan, reunión de clan) ni de `MENTOR_WANTED` (maestro sin discípulo); solo se conectaron `HEIRLOOM_LOST` y `FAMILY_DISHONOR`, que ya tenían plantilla escrita desde la Fase 5.
- El origen de un apellido (`SurnameOrigin`) es información del momento de generarlo; no se persiste junto a la persona.
