# Lifecycle y EventBus 2.0

## Problema

El lifecycle tenía pocos estados y no exponía historial/métricas. EventBus
aislaba listeners, pero no ofrecía prioridad, suscripciones cerrables ni métricas.

## Implementación

`NPCLifecycleState` contiene los estados oficiales de creación, carga, actividad,
pausa, sueño, hibernación, descarga, eliminación y error. El manager valida cada
arista, conserva 256 transiciones, cuenta rechazos y publica un
`NPCLifecycleTransitionEvent` inmutable.

`NPCEventBus` conserva compatibilidad con `subscribe(Class, Consumer)` y añade
prioridades, `EventSubscription`, orden estable global, aislamiento y métricas de
publicación, entrega, errores y latencia. Toda entrega que afecta el dominio
continúa regresando al hilo servidor.

## Pruebas

`CoreTest` verifica transiciones válidas/inválidas, historial, evento tipado,
prioridades, cierre de suscripción, métricas, aislamiento y retorno desde un hilo
externo. La eliminación también cancela diálogo, Brain, queue, tareas globales,
persistencia, entidad, índices, memoria, relaciones y eventos.
