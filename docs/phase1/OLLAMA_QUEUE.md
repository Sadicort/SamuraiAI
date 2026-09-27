# Cola de solicitudes de IA y diálogo

## Problema

`AIRequestQueue` no retenía solicitudes: ante saturación se podían perder, no
había estados, timeout real ni cancelación. El diálogo podía completar turnos
fuera de orden.

## Solución

`AIRequestQueue` usa una `Deque` FIFO protegida, capacidad configurable,
concurrencia máxima, fecha límite de espera y timeout de ejecución. Cada
`Request` expone `PENDING`, `WAITING`, `RUNNING`, `COMPLETED`, `FAILED`,
`CANCELLED` o `TIMEOUT`. `CancellableFutures` propaga `cancel(true)` al proveedor.
Las métricas incluyen cola, activos, completados, fallos, cancelados, timeouts,
latencia media/máxima y latencia del proveedor.

`DialogueService` añade una lane por NPC, identificador de conversación y turno.
Sólo la cabeza de la lane se envía a IA; una respuesta obsoleta (NPC inactivo,
turno incorrecto o runtime sustituido) se descarta. `AIError` clasifica timeout,
modelo ausente, Ollama apagado, HTTP, JSON, cancelación y errores internos para
producir un fallback hablado.

## Prueba

`AIRequestQueueTest` cubre FIFO, backpressure, timeout, cancelación de NPC,
cancelación upstream, concurrencia y cierre. `DialogueTest` cubre orden,
respuesta tardía, cancelación y fallback.

## Riesgos futuros

La prioridad está preparada pero aún es FIFO puro. La memoria larga y el
streaming de respuestas deben añadirse sin saltarse la validación de turno.
