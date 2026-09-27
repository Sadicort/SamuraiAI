# Clanes

**Código:** `living/family/clan/ClanRecord.java`; CRUD y ciclo de vida en `FamilyEngine` (`createClan`, `joinClan`, `leaveClan`, `electClanLeader`, `refreshClanStatus`, `clan`, `clans`, `findClan`); eventos en `living/family/events/Clan*Event.java`; comandos `/samuraiai living family clan …`.

## Por qué no es un segundo Family Engine

Un clan es una **capa social por encima de las familias** (Hogar → Familia → Rama → Clan), no una genealogía paralela. `ClanRecord` no tiene personas, ni parentesco, ni generaciones propias — solo un conjunto de `UUID` de familias miembro (`memberFamilies`). La reputación, el honor, las reliquias y los linajes siguen viviendo en cada `FamilyRecord`; el clan solo lleva la cuenta de a quién pertenece, quién lo lidera y cómo se le ve como conjunto. Por eso vive dentro de `FamilyEngine` (un mapa más, `Map<UUID, ClanRecord> clans`) y se guarda como un bloque más de `families.json`, en vez de un motor o un fichero nuevos.

## Qué guarda un clan

```
id, nombre, fundador (persona), fundado (minuto), región,
familia líder, estado, familias miembro, tradiciones, historia (hasta 100 líneas),
reputación y honor (CauseLedger, igual que una familia), relaciones con otros clanes
```

- **Estado** (`ClanRecord.Status`): `FORMING` (menos de `clanMinFamilies` familias activas, 2 por defecto), `ACTIVE`, `DECLINING`, `DISPERSED` (se quedó sin familias — se recuerda, nunca se borra), `EXTINCT`, `HISTORICAL`. Solo `FORMING`/`ACTIVE` se recalculan solos (`refreshClanStatus`); los otros tres son finales.
- **Relaciones** (`ClanRecord.Relation`): `ALLY`, `NEUTRAL`, `RIVAL`, `HOSTILE`, y dos reservadas para una futura capa de política (`VASSAL_FUTURE`, `OVERLORD_FUTURE`) que nada usa todavía — el mapa `relations()` existe y se persiste, pero ningún código de esta extensión lo escribe automáticamente; es terreno para un comando o un evento futuro.

## Liderazgo, sin herencia obligatoria

Nada dice que el líder de un clan tenga que ser hereditario. `electClanLeader(c)` elige la **familia** (no la persona) con mejor puntuación entre las miembro activas: `reputación + honor/100 + importancia histórica`. Se reelige automáticamente cuando la familia líder deja el clan (`leaveClan`) o deja de estar activa (`refreshClanStatus`) — nunca por turnos ni por antigüedad.

## Ciclo de vida

- **`createClan(nombre, familiaFundadora)`**: una elección deliberada — nada crea un clan por sí solo. La familia fundadora es su primera líder; su cabeza de familia recibe la experiencia `HONOR_OBSERVED` de fundarlo y, si todavía no tiene epíteto, uno de la categoría `FOUNDING` (ver `EPITHET_ENGINE.md`) — fundar un clan es en sí misma una causa real y rastreable.
- **`joinClan(clan, familia)`**: una familia deja cualquier clan anterior antes de unirse a uno nuevo (nunca pertenece a dos a la vez).
- **`leaveClan(clan, familia, razón)`**: si era la líder, se reelige; si era la última familia, el clan pasa a `DISPERSED` y se publica `ClanDisbandedEvent` — pero el registro **se conserva** (`clan(id)` lo sigue devolviendo), tal como pide la especificación.
- **Herencia por rama**: `FamilyEngine.branch(...)` hace que una rama nueva herede el clan de su familia madre automáticamente (además de su cultura de nombre).
- **Extinción**: si la última familia de un clan se extingue (`checkExtinct`), la familia sale del clan por el mismo camino que un abandono voluntario.

## Integración con nombres

Un clan no tiene su propio idioma de nombres: usa el de la familia fundadora. No fuerza una cultura común a sus miembros (dos familias de culturas de nombre distintas pueden compartir clan sin conflicto).

## Persistencia

Los clanes viven en su propio bloque `"clans"` dentro de `families.json` (`FamilyStorage`), con su ledger de reputación y honor (mismo mecanismo que una familia) y sus relaciones. `FamilyEngine.restoreClan(ClanRecord)` los repone al cargar.

## Pruebas

`FamilyIdentityExtensionTest`: `familiesFormAClanItGrowsAndDisbandsWhenTheLastFamilyLeaves` (ciclo de vida completo, incluida la reelección de líder y que un clan disperso se recuerda), `aBranchInheritsItsParentFamilysClan`, y el epíteto de fundador dentro de la misma prueba de ciclo de vida. `identityExtensionSurvivesARestart` cubre la persistencia de la pertenencia y el liderazgo.

## Límites conocidos

- Las relaciones entre clanes (`ALLY`/`RIVAL`/…) se pueden guardar y leer, pero nada las escribe automáticamente todavía — no hay una "guerra de clanes" ni una alianza que surja sola de los eventos existentes.
- No hay ganchos de misión específicos de clan («ceremonia de juramento del clan», «reunión del clan») más allá de los que ya activa la familia — ver `FAMILY_CEREMONIES.md` y la sección de límites de `PHASE5_5_IDENTITY_EXTENSION_CHANGELOG.md`.
