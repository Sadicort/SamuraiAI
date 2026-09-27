# Simulación económica

## Cercanía de jugadores

La economía no distingue niveles de detalle por sí misma: avanza en los pasos que le da la simulación de región (una hora cerca de un jugador, seis horas a media distancia, un día lejos, una semana en tierras olvidadas) y **escala todas sus tasas por la duración del paso**. Lo que sí cambia con la distancia es de dónde vienen las horas de trabajo: rutinas reales del Behavior Scheduler cerca (LOD 0), horas planificadas en el resto.

## Puesta al día

Una región que despierta tras meses se pone al día en como mucho 48 pasos gruesos: la economía produce, consume y se deteriora en esos pasos; los visitantes de los últimos 30 días se sortean; las caravanas en curso avanzan con su propia cadencia.

## Coste

- Paso por asentamiento: trabajadores × receta + consumo + deterioro de lotes + balances; precios solo cada 60 minutos de Deiliora.
- Caravanas: aritmética por paso.
- `planTrade`: una vez al día.
- Todo dentro del presupuesto de simulación del World Engine y del bucket de economía del hub.

Prueba de extremo a extremo: `LivingWorldTest.endToEndTheWorldLivesPersistsAndComesBack` (producir, consumir, escasez, misión, entrega, reinicio).
