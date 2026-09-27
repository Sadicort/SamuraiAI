# Production Engine

**Código:** `living/economy/production/Recipe.java`, `RecipeCatalog.java`; `EconomyEngine.simulate/produce`.

## Horas → mercancías

La producción sale del **incremento** de horas de oficio de cada ciudadano (`Worker.totalHours`, que da la aldea: rutinas reales completadas cerca de jugadores, horas planificadas en simulación abstracta). `accountedHours` recuerda lo ya contado: **cada hora se produce una sola vez**, venga del scheduler o de la simulación. La primera vez que se ve a un trabajador solo se empieza a contar. Como mucho `maxHoursPerDay` (14) horas por día.

## Recetas (por hora)

| Oficio | Fuente | Produce | Consume | Desgaste herramientas | Edificio |
| --- | --- | --- | --- | --- | --- |
| campesino | FARM | arroz 0,9, trigo 0,5 | — | 0,003 | FARM |
| pescador | WILDLIFE | pescado 1,2 | — | 0,002 | |
| cazador | WILDLIFE | carne 0,6, cuero 0,2 | — | 0,003 | |
| tejedor | WILDLIFE (ganado) | tela 0,3 | — | 0,001 | |
| leñador | DEPOSIT | madera 2,0, bambú 0,4 | — | 0,004 | |
| minero | DEPOSIT | piedra 1,8, hierro 0,3, carbón 0,5, arcilla 0,4 | — | 0,006 | MINE |
| herbolario | DEPOSIT | hierbas 0,7 | — | — | |
| herrero | CRAFT | herramientas 0,25 | hierro 0,3, carbón 0,3 | 0,002 | SMITHY |
| carpintero | CRAFT | tablones 0,8 | madera 1,0 | 0,003 | CARPENTRY |
| cocinero | CRAFT | comida 1,2 | arroz 0,6, pescado 0,3 | — | KITCHEN |
| monje, guardia, samurái, mercader, sanador | SERVICE | — | — | — | |

## Factores

- **DEPOSIT:** lo producido se **saca** del depósito de la región (`world.extract`); si el depósito se agota, no hay más.
- **WILDLIFE:** se toma de la fauna de la región (`harvestAnimals`, como mucho `wildlifeShare` 8 %).
- **FARM:** limitado por las granjas (`farmersPerFarm` 6 campesinos por granja) y multiplicado por el calendario agrícola (`productionFactor`: temporada, cosecha fijada por el tiempo).
- **CRAFT:** toma sus entradas del almacén; sin entradas, sin producto.
- **Clima:** el trabajo exterior se multiplica por `outdoorFactor` del clima de la región (los talleres no).
- **Herramientas:** se gastan del almacén; sin herramientas la producción cae a `noToolsFactor` (50 %).
- **Habilidad:** `Skill.multiplier(horas)` (de aprendiz a maestro).
- **Salario:** `wageShare` (30 %) del valor producido pasa del tesoro al trabajador (`WAGE`, `transferUpTo`: nunca más de lo que hay).

Además, **agua** de los pozos: `wellWaterPerDay` (60) por pozo y día, sacada del depósito de agua de la región.

Cada lote producido lleva `Provenance` (`producer`, `region-deposit`, `farm`, `well`…). `ResourceProducedEvent(producido, trabajadores)`.

Pruebas: `gatheringTakesFromTheRegionAndRecordsProvenance`, `workshopsNeedTheirInputs`, `VillageEngineTest.abstractSimulationGivesWorkExperience`.
