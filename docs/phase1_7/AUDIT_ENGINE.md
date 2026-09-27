# Foundation Audit Engine

## Problema

Los informes manuales no constituían un mecanismo reproducible de arranque.

## Implementación

`FoundationAuditEngine` descubre `AuditCheck` mediante `AuditRegistry`.
IDs duplicados o metadatos incompletos se rechazan. Un fallo de discovery o
registro vacío genera BLOCKER. `AuditPipeline` ejecuta las diez etapas en el
orden de `AuditStage` y ordena IDs para obtener resultados deterministas.

Cada resultado incluye ID, etapa, módulo, severidad, outcome, detalle,
recomendación y nanosegundos. Un auditor que lanza una excepción produce FAIL;
los siguientes auditores continúan para completar el diagnóstico. No se oculta
un error convirtiéndolo en PASS. Las etapas vacías generan NOT_RUN.

Auditores iniciales: estructura de responsabilidades del núcleo, versiones del
runtime, tres parámetros críticos de configuración, recursos de arranque,
manifest/binding de voz y versión opcional de CustomNPCs. La comprobación de
certificación declara explícitamente NOT_RUN: no ejecuta pruebas de hardware
ni benchmarks durante el arranque.

## Pruebas y extensión

`FoundationAuditTest` verifica registro, orden, cobertura pendiente, aislamiento,
severidades, discovery real, voz excluida del dedicado, versión opcional no
soportada y reportes. Añadir un nuevo proveedor al archivo de servicios y una
prueba negativa que demuestre el fallo que detecta.

## Riesgos

Los auditores de inicio deben ser pequeños y acotados. No lanzar inferencia,
descargas o benchmarks en el pipeline actual. Faltan timeout por auditor,
catálogo exhaustivo de aceptación y métricas históricas; el reporte muestra
duraciones, no una certificación de rendimiento.
