# Fase 5 — Propiedad de los datos (una sola fuente de verdad)

| Dato | Dueño | Los demás… |
| --- | --- | --- |
| Tiempo oficial, fechas, estaciones, clima, luna, festivales, cronología objetiva | Calendar | guardan minutos de su reloj |
| Regiones, recursos naturales, fauna, asentamientos (existencia y lugar), caminos, eventos de mundo, población agregada, catálogo de oficios | World | referencian por id |
| Aldea (= asentamiento), ciudadanía, edificios, distritos, casas y camas, oficio de cada ciudadano, horarios, seguridad, visitantes, memoria rápida | Village | preguntan por puertos |
| Mercancías (lotes con procedencia), almacenes, precios, monedas, mercaderes, caravanas, rutas comerciales, contratos, impuestos | Economy | piden por puertos; nunca crean |
| Misiones, campañas, cadenas, historia de misiones | Quest | los NPCs no contienen misiones |
| Personas, parentesco (2 aristas), familias, hogares familiares, linajes, aprendizajes, técnicas, herencias, reliquias, legado | Family | — |
| Recuerdos, relaciones, emociones, personalidad, conocimiento aprendido | capa cognitiva (Fase 3) | el mundo vivo solo pide experiencias |
| Conocimiento colectivo, cultura, historia **recordada**, reputación de personas en una comunidad | Knowledge (Fase 3) | cada aldea enlaza una comunidad |
| Hogar físico del NPC | `NPCInstance.home` | la aldea escribe ahí la cama |
| Decisión de qué hace un NPC | Behavior Scheduler | el mundo vivo solo sesga |

## Decisiones de no duplicación

- **Cronología vs historia:** la objetiva es del calendario; la de Knowledge es lo que la gente recuerda (el hub copia solo hechos públicos significativos, como testigo).
- **Aldea vs comunidad:** una aldea enlaza una comunidad; la membresía social y la reputación no se duplican.
- **Reputación:** personal = Knowledge; familiar = Family (expectativa con causas); no se copia a los hijos.
- **Parentesco vs relación:** parentesco en Family; afecto en Relationship. Familia no implica afecto.
- **Casas:** una sola: `HomeRecord` → `NPCInstance.home`; los hogares familiares apuntan a la casa de la aldea.
- **Profesión:** qué es (World), quién la tiene (Village), qué produce (Economy), qué se sugiere a un hijo (Family, nunca copia).
- **Configuración:** se eliminaron claves duplicadas o muertas (`VillageSettings.autoVillages/…`, `WorldSettings.celebrateGreatHarvests`, `LivingSettings.driveVanillaWeather` del adaptador) y se conectaron las que no se leían (`debugLogging`, `contractDays`, `offerRadius`, `threatPerLevel`, `combatThreat`).
