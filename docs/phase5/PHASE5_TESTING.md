# Fase 5 — Pruebas

## Unitarias e integración (JUnit, sin Minecraft) — `src/test/java/yadi/samuraiai/living/**`

| Clase | Pruebas | Qué cubre |
| --- | --- | --- |
| `LivingArchitectureTest` | 4 | motores sin importarse; solo el adaptador toca Minecraft; `core` sin motores; nombres de experiencia válidos |
| `calendar/CalendarEngineTest` | 16 | forma del calendario, reloj monótono (sol y `gameTime`), un año entero de eventos, luna y Tsukimi, clima determinista, temperatura, cosecha por clima, aniversarios, cronología, tiempo offline, reinicio |
| `world/WorldEngineTest` | 9 | regiones, depósitos, fundaciones y caminos, guerra y bloqueos, resolución de eventos, streaming y puesta al día, fauna, eventos deterministas, reinicio |
| `village/VillageEngineTest` | 9 | plano, oficios y camas, horarios distintos, sesgo y reacción a ataques y lluvia, seguridad, mercado, visitantes, simulación abstracta, reinicio |
| `economy/EconomyEngineTest` | 9 | procedencia, talleres, consumo/escasez/precios/contratos, caravanas, emboscadas y bloqueos, contratos, dinero nunca creado, confianza en precios, reinicio |
| `quest/QuestEngineTest` | 9 | escasez → misión, entrega y pago, caminos, confianza, defensa, giros/fusiones/caducidad, resolución por el mundo, campañas y cadenas, reinicio |
| `family/FamilyEngineTest` | 12 | antepasados, hogares, parentesco, genealogía imposible, nacimientos y edad, sucesión/herencia/legado, maestros y escuelas, saber perdido, oficios sugeridos, reputación/honor/tradiciones/historias, extinción, reinicio |
| `LivingWorldTest` | 7 | extremo a extremo con reinicio, caravana entre aldeas, guerra, región abandonada, nacimientos y muertes en la población, disputa → rencilla → campaña, maestro de oficio y migración |
| `LivingPerformanceTest` | 1 | carga de 200 ciudadanos 10 días a velocidad real |
| `ai/scheduler/ExternalBiasTest` | 2 | fuentes de sesgo externas del scheduler |
| `prompt/WorldSectionTest` | 2 | sección «TU MUNDO» del prompt |

## GameTests (servidor real) — `src/gametest/java/yadi/samuraiai/gametest/LivingGameTests.java`

| Prueba | Qué comprueba |
| --- | --- |
| `anNpcBecomesACitizenWithABedAFamilyAndATimetable` | NPC real → ciudadano, cama → hogar, familia, fuente de sesgo del scheduler, prompt con aldea y fecha |
| `schedulerZonesBecomeBuildingsAndAreAbandonedWhenRemoved` | zona → edificio; zona borrada → abandonado (y re-creada → construido) |
| `aHousesRealBedBecomesItsResidentsHomeAndItsCampfireIsKnown` | escaneo de una casa, cama real → hogar, hoguera como punto de interacción |
| `theLivingCommandsAnswer` | 18 comandos `/samuraiai living` responden en un servidor real |
| `theCalendarFollowsTheServerAndEverythingIsWrittenToTheWorld` | el reloj sigue al servidor; los ficheros se escriben en el mundo |

El mundo vivo está **dormido** en el servidor de GameTest (propiedad `samuraiai.gameTests`) para que las pruebas físicas de otros motores midan solo esos motores; su lote lo despierta con `@BeforeBatch` y lo duerme con `@AfterBatch` (las pruebas de un lote corren en paralelo).

## Resultados (26-27 de septiembre de 2026)

- `./gradlew.bat test --offline`: **484 pruebas, 0 fallos**, 1 omitida (la de Whisper nativo, preexistente).
- `./gradlew.bat check --offline` (sin CustomNPCs) y `./gradlew.bat check -PwithCustomNpcs=true --offline`: **26/26 GameTests** en ambos perfiles, más `verifyDistributionJar`; dos ejecuciones seguidas sobre el mismo mundo de pruebas (reinicio real) también en verde.
- El suite del mundo vivo repetido 6 veces seguidas sin fallos (búsqueda de inestabilidad).

## Inestabilidades encontradas y corregidas

- Prueba de rumores de la Fase 3: dejaba sus NPCs guardados en el mundo de pruebas al tener éxito (la limpieza diferida no se ejecuta si la prueba termina antes) → rompía la siguiente ejecución.
- Prueba de emboscada: dependía de una probabilidad de 0,95.
