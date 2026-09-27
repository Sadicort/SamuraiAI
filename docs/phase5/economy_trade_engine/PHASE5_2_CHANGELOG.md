# Changelog — Fase 5.2 Economy & Trade

## Añadido

- `living/economy`: 19 recursos, 15 recetas por oficio (depósito, fauna, granja, taller, servicio), consumo por persona con estación, almacenes con lotes FIFO y procedencia, libro de movimientos, riqueza con acuñación única registrada, precios dinámicos con 6 factores, mercados con existencias finitas, mercaderes con personalidad, caravanas en segundo plano con emboscadas y retrasos, rutas invalidadas por evento, contratos, impuestos, escasez/excedente con histéresis, memoria comercial, 22 eventos, métricas, inspector, 3 secciones persistentes con revisión propia.
- Adaptador: comercio de jugadores con objetos (`ResourceItems`), monedas, entregas de misión, contratos.

## Corregido en la integración

- Los contratos solo se creaban desde pruebas; ahora la escasez los ofrece y los jugadores los cumplen (`contractDays` pasa a usarse).
- Secciones de economía que compartían un indicador de suciedad → contador de revisión por sección.
- Prueba de emboscada dependiente del azar (0,95) → hasta 5 caravanas independientes.

## Límites

Sin entidades de caravana ni carros físicos; el oro y la plata existen pero nada los produce; las monedas del jugador no son objetos.
