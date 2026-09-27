# Diagnóstico inicial

## Implementado

`FoundationBootstrap` captura versiones de mods, distribución, Java, sistema
operativo, arquitectura, procesadores disponibles, límite de heap, hilo de
auditoría y tres valores de configuración. Los mapas de `AuditContext` son
inmutables. No incluye nombre de usuario, texto de conversaciones, audio,
credenciales ni tokens.

`AuditSummary` agrega decisiones de inicio y módulos deshabilitados. Los
resultados incluyen duraciones y recomendaciones. `AuditReporter` persiste el
informe en la carpeta administrada de Foundation.

## Pendiente

No se ha implementado diagnóstico exhaustivo de GPU, permisos, disco, red,
latencias ni histórico. No se hace una llamada a Ollama desde el auditor; el
diagnóstico asíncrono existente del servidor permanece independiente.

## Prueba y riesgo

Arrancar cliente/dedicado y revisar `config/samuraiai/foundation` dentro del
directorio de ejecución. Un error de escritura queda en el log de Foundation;
no convierte la auditoría en aprobada ni proporciona certificado persistido.
El JSON y Markdown comparten `runId` para detectar reportes de ejecuciones
distintas si se interrumpe la escritura del par.
