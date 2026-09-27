# Resource Engine

**Código:** `living/economy/resources/ResourceCatalog.java`, `ResourceDef.java`.

Cada recurso: categoría (`FOOD, WATER, MATERIAL, FUEL, TOOL, TEXTILE, MEDICINE, LUXURY`), rareza, peso por unidad (lo que carga un carro), valor base en monedas, deterioro diario, estaciones de cosecha, oficios que lo producen, quién lo consume, valor como comida/combustible y `future` (existe pero nada lo produce aún).

| Id | Nombre | Categoría | Valor | Deterioro/día | Productor |
| --- | --- | --- | --- | --- | --- |
| rice | Arroz | FOOD | 2 | 0,002 | campesino |
| wheat | Trigo | FOOD | 1,8 | 0,002 | campesino |
| fish | Pescado | FOOD | 2,5 | 0,08 | pescador |
| meat | Carne | FOOD | 3,5 | 0,06 | cazador (y ganado) |
| meal | Comida preparada | FOOD | 3 | 0,25 | cocinero |
| water | Agua | WATER | 0,05 | 0 | pozos |
| wood | Madera | FUEL | 1 | 0 | leñador |
| coal | Carbón | FUEL | 3 | 0 | minero |
| iron | Hierro | MATERIAL | 8 | 0 | minero |
| stone | Piedra | MATERIAL | 0,8 | 0 | minero |
| cloth | Tela | TEXTILE | 6 | 0 | tejedor |
| leather | Cuero | TEXTILE | 5 | 0,002 | cazador |
| bamboo | Bambú | MATERIAL | 1,2 | 0 | leñador |
| herbs | Hierbas | MEDICINE | 4 | 0,03 | herbolario |
| clay | Arcilla | MATERIAL | 0,8 | 0 | minero |
| tools | Herramientas | TOOL | 15 | 0 | herrero |
| lumber | Tablones | MATERIAL | 2,5 | 0 | carpintero |
| gold, silver | Oro, Plata | LUXURY | 120, 60 | 0 | `future` |

Líneas configurables (`resources`): `rice;name=Arroz;category=FOOD;rarity=0.1;weight=1;value=2;spoil=0.002;food=1.0;seasons=AUTUMN;producers=farmer;consumers=people,cook`.

## Recursos y objetos de Minecraft

El adaptador (`living/server/ResourceItems.java`, clave `resourceItems` de `samuraiai-living.toml`) define qué objetos representan cada recurso para comerciar y entregar: arroz y trigo → trigo; pescado → bacalao/salmón; carne → ternera/cerdo/cordero/pollo/conejo; comida → pan/patata asada/estofado; madera → troncos; carbón → carbón/carbón vegetal; hierro → lingote; piedra → roca/piedra; tela → lana blanca/cuerda; cuero; bambú; hierbas → helecho/flores/bayas; arcilla → bola de arcilla; herramientas → herramientas de hierro; tablones → tablones; oro → lingote de oro. **Agua y plata no cruzan**: no se inventa un objeto para ellas. Un objeto = una unidad.
