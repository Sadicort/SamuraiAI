# Fase 5 — Informe final

## Estado

Los seis motores de la Fase 5 (5.0–5.5) están implementados como **una** arquitectura (motores puros + hub + adaptador), integrados entre sí y con las fases anteriores (scheduler, cognición, percepción, diálogo, persistencia, configuración), persisten y sobreviven a reinicios, publican eventos, tienen métricas, depuración, pruebas y documentación. Compilan y pasan todas las pruebas en ambos perfiles (`PHASE5_TESTING.md`).

## Definición de hecho — verificación

| Área | Criterio | Estado |
| --- | --- | --- |
| Mundo | regiones persisten; simulación; regiones lejanas abstractas; eventos de mundo; historia persiste | cumplido (pruebas de mundo, LOD, puesta al día, reinicio) |
| Aldeas | ciudadanos con horario; casas; edificios funcionales; oficios; reaccionan a eventos; memoria comunitaria persiste | cumplido; los edificios son registros y zonas, **no** estructuras levantadas por el mod |
| Economía | producción y consumo reales; procedencia; almacenes persisten; mercados; precios variables; caravanas; rutas; escasez/excedente | cumplido; caravanas sin entidades |
| Misiones | emergen del estado real; objetivos dinámicos; caminos; el fracaso tiene consecuencias; campañas e historia persisten | cumplido |
| Calendario | fecha oficial; años/meses/días; estaciones; clima integrado; festivales; cronología persiste; el tiempo no retrocede | cumplido; el cielo vanilla solo si `driveVanillaWeather` |
| Familias | persisten; genealogía; padres/hijos; generaciones; hogares; maestros/discípulos; sucesión | cumplido; nacimientos solo a propósito, muerte natural no simulada |

## Integración con el juego

- NPCs reales → ciudadanos, familias, casas (cama física), oficios, horarios (sesgo del scheduler), muertes reales, amenazas.
- Jugadores → misiones (hablar, llegar, meditar, matar, defender, entregar), comercio con objetos, contratos, monedas, noticias, HUD, prompts de diálogo.
- Mundo físico → biomas (tipo de región), zonas → edificios, camas, hogueras, cultivos.

## Correcciones hechas durante la integración (fuera y dentro de la Fase 5)

- Scheduler: fuente de sesgo externo (`RoutineBiasSource`), `rehome`.
- `NPCInstance.homeAssigned()`: el hogar por defecto (punto de aparición) no impedía la cama de la aldea.
- Amenaza de percepción en escala equivocada; `combatThreat`, `threatPerLevel`, `contractDays`, `offerRadius` y los 7 `debugLogging` no se leían; claves duplicadas eliminadas.
- Puertos conectados sin uso (`Relations` → precios por confianza, `tradeFactor` → carga de caravanas) y métodos de puerto muertos eliminados.
- Condiciones `DISPUTE`/`GRUDGE` nunca informadas → ahora desde relaciones cognitivas y familias rivales.
- Nombres de experiencia inexistentes (`BETRAYED_ME`, `WITNESSED_HEROISM`) que se descartaban en silencio.
- Nacimientos que no llegaban al registro de población; técnicas de oficio y migración familiar sin disparador.
- Trabajo del día repartido en 4 ticks.
- Prueba de rumores de la Fase 3 que ensuciaba el mundo de pruebas.

## Límites honestos (no hechos o parciales)

1. **No hay «Battlefield Engine»** ni fase de combate avanzada en el código: la guerra son eventos de mundo; el combate real de NPCs solo sube la amenaza de la aldea.
2. **No se construyen estructuras**: el plano de una aldea crea edificios `PLANNED`; los edificios reales vienen de zonas del scheduler o de comandos.
3. **Caravanas, visitantes y ciudadanos sin cuerpo no tienen entidades**; no se generan NPCs nuevos para ellos.
4. **Nacimientos** solo por comando/API; **muerte natural** no simulada; los niños no tienen cuerpo; las etapas infantiles son `_FUTURE`.
5. **Reliquias** sin objeto físico estampado; **monedas** del jugador sin objeto (cuenta).
6. **Cielo vanilla** no gobernado por defecto (`driveVanillaWeather = false`).
7. **Sin canal de red propio**: el calendario llega al cliente por chat/barra de acción, no por una GUI.
8. **Interacciones físicas** (fuegos, cultivos) solo con un jugador en la región (`FULL`); no probadas en GameTest por falta de jugadores allí.
9. El **tráfico** de visitantes usa un valor fijo; `SEAT`, `WELL` y `USE_WORKSTATION` están en el modelo pero no se usan; `TITLE` como recompensa y la parte de facción de los impuestos están preparados sin efecto.
10. Rendimiento medido en JVM de pruebas; falta una prueba de carga en servidor con muchos NPCs con cuerpo y varios jugadores.
11. Ninguna de estas piezas se ha probado aún en una partida jugada por una persona (solo pruebas automáticas y GameTests).

## Siguientes pasos recomendados

Prueba de juego real con jugadores; decidir si `driveVanillaWeather` debe estar activo; estampar reliquias en objetos; nacimientos y muerte natural cuando existan NPCs niños; conectar un futuro motor de combate al puerto de conflicto; prueba de carga en servidor.
