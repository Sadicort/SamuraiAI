# Festival Engine

**Código:** `living/calendar/festivals/FestivalCatalog.java`, `FestivalDef.java`, `FestivalEngine.java`.

## Datos

Cada festival es una línea: `tsukimi;name=Festival de la Luna;month=8;day=12;days=1;moon=FULL;window=6;bias=PRAYER:30,MEDITATE:20,SOCIAL:25,WORK:-10;demand=RICE:1.2,HERBS:1.1;social=1.3;tags=autumn,moon,festival` (opcional `cultures=...`).

| Integrado | Fecha | Días | Sesgo de rutinas | Demanda |
| --- | --- | --- | --- | --- |
| Hanami | 10 de Kisaragi | 4 | SOCIAL +40, PRAYER +10, WORK −15, TRAINING −10 | arroz, tela, hierbas |
| Festival de Verano | 20 de Satsuki | 3 | SOCIAL +45, MERCHANT +20, WORK −20, PATROL +5 | pescado, arroz, tela |
| Festival de la Luna | desde el 12 de Hazuki, primera luna llena en 6 días | 1 | PRAYER +30, MEDITATE +20, SOCIAL +25 | arroz, hierbas |
| Festival de la Cosecha | 5 de Nagatsuki | 3 | SOCIAL +40, PRAYER +20, MERCHANT +25 | arroz, trigo, carne |
| Festival de Invierno | 20 de Shimotsuki | 3 | SOCIAL +35, PRAYER +25, REST +10 | madera, arroz, tela |

Las líneas de `festivals` en `samuraiai-calendar.toml` sustituyen un festival con el mismo id o añaden otros; `cultures` limita un festival a esas culturas de aldea.

## Motor

- La fecha de inicio de cada festival en cada año se calcula una vez y se guarda en caché; un festival lunar empieza el primer día de su ventana con la luna de la familia pedida (o al final de la ventana si no llega).
- `activeOn(día)`, `activeToday()`, `onDay(día)` (empiezan / terminan), `next(día)` (próximo festival y días que faltan).
- Eventos: `FestivalStartedEvent`, `FestivalEndedEvent`; el comienzo se anota en la cronología (`FESTIVAL`, significancia 0,3).

## Efectos reales

- **Horarios:** el Village Engine suma el `routineBias` del festival (× `festivalBiasScale`) al sesgo que llega al Behavior Scheduler.
- **Economía:** la demanda de cada recurso se multiplica mientras dura (`EconomyPorts.Calendar.festivalDemand`).
- **Vida social:** `social` sube la actividad social de la aldea.
- **Misiones:** `ConditionScanner` abre «preparativos del festival» `festivalWarningDays` (3) días antes en las aldeas que lo celebran (entregar el recurso que más demanda).
- **Aldeas:** cada día que hay festival, cada aldea cuya cultura lo celebra abre un evento de aldea `FESTIVAL` (con el sesgo escalado), gana renombre (+0,5) y cuenta el festival en su memoria (`Village.Counter.FESTIVALS`).
- **Anuncio:** los jugadores conectados ven «[Deiliora] Comienza Hanami».

## Pruebas

`advancingAYearAnnouncesDaysSeasonsFestivalsHolidaysAndTheNewYear`, `theMoonFestivalFallsOnAFullMoon`.
