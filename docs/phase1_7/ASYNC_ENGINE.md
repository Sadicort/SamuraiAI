# Global Async Engine y Thread Guard

## Problema

Voice y la cola de IA creaban executors independientes. La cancelación no tenía
propietario global ni existían métricas comunes, prioridad o backpressure.

## Implementación

`foundation.async.AsyncEngine` es el único creador de pools en producción:
workers daemon limitados, cola con prioridad, admisión acotada, deadlines,
timeouts, cancelación por propietario, interrupción y métricas. `AsyncTaskHandle`
propaga la cancelación del future. `ThreadGuard` valida fronteras cliente,
servidor y worker.

`VoiceThreadDispatcher` delega en AsyncEngine. `VoiceRecorder` abre el micrófono
en worker, no en el hilo cliente, y borra buffers PCM. Los deadlines de
`AIRequestQueue` usan el scheduler central. Al eliminar un NPC se cancelan sus
tareas con propietario `npc:<uuid>`.

## Pruebas

`AsyncEngineTest` cubre prioridad FIFO dentro de prioridad, saturación, timeout
con interrupción, cancelación simultánea de tareas queued/running y métricas.
La suite completa, ambos GameTests y Whisper real pasaron tras la migración.
Operaciones nativas no interruptibles se cierran después de su inferencia activa.
