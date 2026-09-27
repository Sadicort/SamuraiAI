# Ceremonias familiares

**Código:** llamadas a `social.experience(...)` dentro de `FamilyEngine` (interfaz `FamilyPorts.Social`, implementada por `LivingBridges` hacia la capa cognitiva).

## El principio: ninguna ceremonia nueva, ningún motor de emoción nuevo

Una ceremonia familiar no es un sistema aparte: es, sencillamente, que un evento familiar real (una sucesión, una graduación, una entrega de reliquia, la fundación de un clan) se anuncie también a la capa cognitiva como una experiencia con nombre (`ExperienceKind`), para que la emoción, la relación, la memoria o la reputación de las personas implicadas reaccionen con la maquinaria que **ya existe** desde la Fase 3. `FamilyEngine` no calcula emociones ni relaciones — solo llama a `social.experience(...)` con el verbo correcto en el momento correcto.

## Qué ceremonias están conectadas hoy

| Ceremonia | Dónde | Verbo cognitivo | A quién |
| --- | --- | --- | --- |
| Sucesión al frente de una familia | `FamilyEngine.succeed` | `HONOR_OBSERVED` | el nuevo cabeza de familia |
| Transición de maestro (sucesión de un linaje/escuela) | `FamilyEngine.lineageSuccession` | `HONOR_OBSERVED` | el nuevo maestro |
| Fin de un aprendizaje (graduación) | `FamilyEngine.endMentorship` (estado `COMPLETED`) | `CELEBRATED` | tanto el discípulo como el maestro |
| Entrega de una reliquia | `FamilyEngine.transferHeirloom` | `GIFT_RECEIVED` | quien la recibe |
| Concesión de un epíteto | `FamilyEngine.considerEpithet` | `HONOR_OBSERVED` o `DISHONOR_OBSERVED` según la categoría | la persona |
| Fundación de un clan | `FamilyEngine.createClan` | `HONOR_OBSERVED` (y un epíteto `FOUNDING` si aún no tenía ninguno) | el cabeza de la familia fundadora |

Cada verbo (`"HONOR_OBSERVED"`, `"CELEBRATED"`, `"GIFT_RECEIVED"`, `"DISHONOR_OBSERVED"`) es un nombre real del catálogo `ExperienceKind` de la capa cognitiva — nunca un texto inventado. `LivingArchitectureTest.everyExperienceNameExistsInTheCognitiveCatalogue` escanea el código fuente en busca de toda llamada `social.experience(...)` y comprueba que el nombre usado exista de verdad en ese enumerado; ninguna de las llamadas añadidas por esta extensión puede pasar la compilación de ese test si usara un nombre inventado.

## Ganchos de misión: de la familia al Quest Generator existente

La especificación pide que ceremonias y sucesos familiares puedan también dar pie a misiones, sin crear un segundo motor de misiones. `QuestTemplates` ya tenía, desde la Fase 5, plantillas pensadas exactamente para esto (`recover_heirloom` para `ConditionKind.HEIRLOOM_LOST`, `restore_honor` para `ConditionKind.FAMILY_DISHONOR`) — pero **nada las disparaba nunca**: no había ningún código que reportase esas condiciones al motor de misiones. Esta extensión cerró ese hueco:

- **`HeirloomLostEvent`** (nuevo, publicado por `FamilyEngine.heirloomLost` cuando una reliquia se marca como perdida) → `LivingReactions` reporta `ConditionKind.HEIRLOOM_LOST` con el nombre de la reliquia y de la familia como variables, así que la plantilla `recover_heirloom` («{heirloomName} de la familia {familyName}») ya puede generarse de verdad. Esto es literalmente el «recuperar un arma ancestral» que pide la especificación.
- **`FamilyReputationChangedEvent`** con causa de honor, cuando el honor de una familia cruza hacia abajo un umbral de deshonor → `LivingReactions` reporta `ConditionKind.FAMILY_DISHONOR`, activando `restore_honor` («El honor de la familia {familyName}»). Esto es el «restaurar la reputación de la casa» de la especificación.

En ambos casos, quien da la misión (`giver`) es un familiar vivo presente como ciudadano en alguna aldea — nunca un desconocido — siguiendo el mismo patrón que ya usaba `MISSING_PERSON` para una persona desaparecida.

## Qué no está conectado

- **Aniversario de fundación de una familia** y **ceremonia de juramento**: la especificación las menciona como ejemplos de ceremonia, pero ninguna de las dos tiene hoy un disparador natural en el código (no hay noción de "aniversario" en el calendario ligada a una familia, ni un mecanismo de juramento en ningún otro motor al que engancharse). No se improvisó uno para no arriesgar un disparador frágil o mal fundamentado; queda como trabajo futuro con un caso de uso concreto que lo pida.
- **Ganchos de misión específicos de clan** («resolver una disputa de clan», «reunión del clan»): un clan puede tener honor y reputación propios (`CLAN_ENGINE.md`), pero ningún evento de clan reporta todavía una `WorldCondition` al Quest Generator. `ConditionKind.DISPUTE` existe pero, igual que ocurría con `HEIRLOOM_LOST`/`FAMILY_DISHONOR` antes de esta extensión, no tiene quien lo dispare.
- **`MENTOR_WANTED`** (un maestro sin discípulo que envejece) sigue igual de sin disparador que antes de esta extensión — sin código nuevo, sacarlo de la Fase 5 quedaba fuera del alcance de este trabajo.

## Pruebas

Las seis llamadas de ceremonia están cubiertas indirectamente por las pruebas existentes de sucesión, mentoría, herencia y por `FamilyIdentityExtensionTest` (epítetos, clanes). El test de arquitectura (`LivingArchitectureTest.everyExperienceNameExistsInTheCognitiveCatalogue`) protege a todas por igual frente a un nombre de experiencia inventado. Los dos ganchos de misión (`HEIRLOOM_LOST`, `FAMILY_DISHONOR`) se verificaron por compilación y por la batería completa de GameTests (`check` en los dos perfiles de compilación) — no tienen una prueba unitaria dedicada porque `LivingReactions` solo se ejercita con el mundo vivo completo, igual que el resto de reacciones del hub (`CaravanLostEvent`, `ScarcityStartedEvent`, etc.).
