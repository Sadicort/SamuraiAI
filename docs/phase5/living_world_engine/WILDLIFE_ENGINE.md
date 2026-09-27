# Wildlife Engine

**Código:** `living/world/wildlife/WildlifeEngine.java`, `SpeciesDef.java`, `WildlifePopulation.java`.

Poblaciones animales por región, simuladas como **agregados** (no se generan ni se necesitan entidades):

- **Crecimiento logístico** hacia la capacidad de la región, escalado por la actividad animal de la estación (primavera 1,2, invierno 0,6). El paso logístico se resuelve en forma cerrada: una región dormida un año cuesta lo mismo que una simulada ayer.
- **Depredadores** comen a sus presas (`prey`, `predation` por día).
- **Plagas** (`pest=true`, ratas) crecen con el excedente de comida de la aldea (`Environment.foodSurplus`).
- **Personas** toman animales cazando, pescando o criando: la economía pide `harvestAnimals` a través del hub y recibe carne, cuero, pescado o tela según `yields`.
- **Peligro:** `dangerPer100` suma peligro a la región (lobos).

## Especies integradas

| Especie | Hábitat (capacidad) | Crecimiento/día | Rinde |
| --- | --- | --- | --- |
| Ciervos | bosque 120, campos 50, montaña 40 | 0,02 | carne 3, cuero 1 |
| Lobos | bosque 18, montaña 24 | 0,01; comen ciervos (0,15) | cuero 1; peligro 0,6 |
| Aves | bosque 300, campos 200, río 150, pantano 250, costa 200 | 0,04 | — |
| Peces | río 900, costa 1500, pantano 300 | 0,03 | pescado 1 |
| Ganado (doméstico) | aldea 60, campos 80 | 0,012 | carne 2, cuero 1, tela 0,5 |
| Ratas (plaga) | aldea 200, templo 40 | 0,05 | — |

Líneas configurables (`wildlife`): `deer;name=Ciervos;habitat=FOREST:120,FIELDS:40;growth=0.02;yield=meat:3,leather:1`.

## Cuándo se simula

Como `RegionSimulator` interno del World Engine: cada paso de la región (una hora cerca de un jugador, una semana en tierras olvidadas). Prueba: `wildlifeGrowsLogisticallyAndPredatorsEat`.
