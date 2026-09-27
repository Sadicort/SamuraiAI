# Reportes Foundation

`AuditReporter` escribe en `config/samuraiai/foundation/` del perfil ejecutado:

- `FOUNDATION_STATUS.json`: snapshot estructurado del arranque.
- `FOUNDATION_AUDIT.md`: tabla legible con resultados y recomendaciones.

Los documentos homónimos de `docs/phase1_7` son registros de ingeniería,
no la salida del runtime. No se sobrescriben con los reportes del juego.

Cada archivo se escribe a un temporal único y se sustituye atómicamente.
Ambos contienen un mismo ID de ejecución; el par no es una transacción única.
Si el filesystem no soporta sustitución atómica, se registra un error y no se
publica un archivo parcial. Los temporales propios se limpian en `finally`.

El estado de inicio puede ser BLOCKED, FAILED o PARTIAL. No se produce READY:
este pipeline no ejecuta toda la certificación de la guía. `phase2Unlocked`
es siempre false. NOT_RUN no equivale a PASS; NOT_APPLICABLE debe explicarse
por el perfil, por ejemplo Voice en dedicado.

`FoundationAuditTest.reporterWritesParseableJsonAndMatchingMarkdownRunId`
comprueba sintaxis, coherencia de IDs y ausencia de temporales sobrantes.
La certificación firmada del JAR y el catálogo completo de evidencias siguen
pendientes.
