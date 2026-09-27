# Behavior Engine v2.0 — Requisitos de entrada

Fecha de revisión: 2026-09-19.

## Resultado

La nueva guía exige **Foundation Complete, Fase 1.7 READY**. Actualmente no se
cumple: `../phase1_7/FOUNDATION_STATUS.json` declara `foundation: PARTIAL`,
`certified: false` y `phase2Unlocked: false`. El informe de arranque bajo
`run/config/samuraiai/foundation/FOUNDATION_STATUS.json` también mantiene PARTIAL.
Que `startupAllowed` sea verdadero permite arrancar; no certifica Foundation.

Por tanto, no se inicia la implementación del Behavior Engine v2.0 ni se cambia
artificialmente el estado a READY. Esta revisión solo modifica documentación.

## Corrección del diagnóstico histórico

[PRECONDITIONS_REVIEW.md](PRECONDITIONS_REVIEW.md) describe la revisión inicial
del 18 de septiembre, no el estado posterior a las reparaciones. Sus fallos
de CustomNPCs y del bootstrap de voz no deben presentarse como fallos actuales
sin volver a reproducirlos.

Según [el registro de Foundation](../phase1_7/CHANGELOG_PHASE1_7.md):

- Se corrigió el acceso a clases cliente desde CustomNPCs en servidor dedicado.
- Se verificaron arranque y GameTests con y sin CustomNPCs en desarrollo.
- Se corrigieron instalación verificada del modelo, carga asíncrona,
  dependencias nativas y empaquetado/reofuscación.
- Whisper cargó y transcribió una muestra de referencia en el cliente.
- La última batería registrada tiene 56 pruebas unitarias aprobadas y una
  omitida; no equivale a certificación completa ni a pruebas de captura real.

No se repitieron esas pruebas en esta revisión documental. Los resultados
anteriores y sus límites están documentados en el registro citado.

## Qué falta para habilitar Fase 2

| Área | Evidencia o trabajo pendiente |
|---|---|
| Módulos | Implementado y probado; falta incorporarlo a la certificación final del artefacto |
| Concurrencia | AsyncEngine, propiedad/cancelación y auditor de arranque implementados; falta stress y auditoría exhaustiva |
| Auditoría | Completar comprobaciones de lados, módulos, recursos, runtime y rendimiento |
| Voz | Completar requisitos abiertos y comprobar micrófono, GUI, inserción en chat y Push To Talk |
| Actualizaciones | Implementar actualizaciones verificadas; definir origen y clave pública de confianza para manifests firmados |
| Distribución | Validar el JAR final en instalación limpia, con perfiles opcionales y modo offline |
| Rendimiento | Ejecutar los benchmarks y pruebas de carga exigidos, con hardware y resultados registrados |
| CustomNPCs | Resolver o evaluar explícitamente los avisos de recursos restantes del artefacto suministrado |
| Certificación | Vincular resultados verificables al artefacto final; no convertir pendientes u omisiones en PASS |

Las pruebas interactivas necesitan un micrófono disponible y validación dentro
de Minecraft. Para las actualizaciones firmadas hace falta definir quién
publica el manifest y cuál es su clave de confianza; no debe inventarse.

## Aplicación de la nueva guía después de READY

Reutilizar Brain, registro, Tasks, Actions, controladores e integración actuales.
No crear un segundo Brain ni un registro paralelo. La cantidad de clases no
sustituye responsabilidades claras o pruebas físicas.

Orden de implementación previsto:

1. Contexto, lifecycle, registro, planners, scheduler, transiciones y métricas.
2. Actions con resultados reales, cancelación y adaptadores de movimiento,
   mirada y navegación.
3. Patrulla con ancla y progreso local; seguimiento y observación.
4. Investigación, guardia, escolta, wander, descanso, postura y meditación.
5. Percepción, entorno, prioridades por emoción/personalidad y depuración.
6. GameTests que verifiquen desplazamiento, obstáculos, interrupciones,
   replanificación y limpieza; documentación por sistema implementado.

Combate avanzado y memoria persistente permanecen fuera de esta fase.

## Recomendaciones adicionales

- Probar que un controlador sin cuerpo devuelve una operación no soportada,
  nunca éxito de movimiento.
- Asociar acciones a una generación del runtime para rechazar resultados de
  un NPC eliminado o reactivado.
- Medir presupuestos y latencias del scheduler antes de declarar soporte para
  cientos de NPCs.
- Verificar que cancelar una tarea detiene también la navegación física.

Estas recomendaciones no se han implementado en esta revisión.
