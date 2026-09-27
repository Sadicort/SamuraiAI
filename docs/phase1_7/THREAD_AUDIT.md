# Thread Audit

`ThreadReportWriter` genera `THREAD_STATUS.json` y `THREAD_AUDIT.md` en
`config/samuraiai/foundation` durante bootstrap. Registra estado, threads
SamuraiAI, tareas activas, latencias, errores, cancelaciones, timeouts, rechazo
por backpressure y deadlocks detectados por `ThreadMXBean`.

READY en este reporte significa que el snapshot no encontró deadlocks ni
rechazos; no es el certificado Foundation. `ThreadReportWriterTest` comprueba
salida JSON/Markdown y `ThreadAudit` valida capacidad del AsyncEngine.
