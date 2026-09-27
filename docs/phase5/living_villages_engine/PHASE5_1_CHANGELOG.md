# Changelog — Fase 5.1 Living Villages

## Añadido

- `living/village`: aldea con identidad del asentamiento, plano determinista (plaza, templo, mercado, talleres, dojo, campos, casas, pozos, puertas), trazado de calles, 7 distritos, 18 tipos de edificio con zona del scheduler, ciudadanos con/sin cuerpo, asignación de oficios por tipo/necesidad/cuota, casas y camas, 4 plantillas culturales y 15 ajustes de oficio, vida de mercado, templo, guardia y social, visitantes, seguridad de 5 estados, 9 tipos de evento de aldea, memoria comunitaria enlazada con Knowledge, censo, malestar y prosperidad, sesgo de rutinas para el Behavior Scheduler, 18 eventos, métricas, inspector, persistencia por aldea.
- Adaptador: ciudadanía al activarse un NPC, muerte real, cama → hogar (`NPCInstance.homeAssigned`, `SchedulerService.rehome`), camas físicas, zonas del scheduler → edificios, amenaza desde percepción y combate, anuncios a jugadores.

## Corregido en la integración

- Claves de configuración duplicadas con `samuraiai-living.toml` eliminadas (`autoVillages`, `autoVillageCell`, `joinRadius`, `villageRadius`, `planNewVillages`).
- La amenaza de la percepción llegaba en escala 0..1 a un umbral en puntos; ahora usa `threatPerLevel`, y `combatThreat` se aplica al combate real.

## Pruebas

9 unitarias (`VillageEngineTest`), GameTests de ciudadanía, zonas y camas.
