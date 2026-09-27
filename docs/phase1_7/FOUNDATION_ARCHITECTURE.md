# Foundation — Arquitectura implementada y límites

## Problema y solución

No existía un punto común de auditoría previa al runtime. Se añadió
`foundation.FoundationBootstrap` y el paquete `foundation.audit`, conservando
Brain, Runtime, Dialogue, controladores y sus paquetes actuales.

En `FMLCommonSetupEvent`, Forge entrega un contexto de distribución física,
versiones y configuración inmutable al auditor. El registro descubre proveedores
mediante `ServiceLoader`. El pipeline produce resultados tipados y el reporter
escribe JSON/Markdown. El runtime servidor y el bootstrap de voz consultan ese
resultado antes de iniciarse.

BLOCKER impide iniciar SamuraiAI, no cierra Minecraft. CRITICAL deshabilita el
módulo afectado; ERROR deja constancia sin detener otros módulos. Una etapa sin
auditor figura como NOT_RUN. Los reportes de inicio nunca conceden certificación
ni desbloquean Fase 2.

## Clases y extensión

`AuditCheck`, `AuditRegistry`, `AuditPipeline`, `AuditContext`, `AuditResult`,
`AuditSummary`, `AuditReporter`, `FoundationAuditEngine`, enums y auditores bajo
`audit.checks`. Para añadir un auditor, implementar `AuditCheck` y registrarlo
en `META-INF/services/yadi.samuraiai.foundation.audit.AuditCheck`.

## Riesgos y pruebas

Esto es el primer núcleo de auditoría, no Foundation Complete. Faltan auditores
exhaustivos de bytecode/lados, módulo loader completo, todas las rutas asíncronas,
rendimiento y certificación integral. No se crean clases vacías para aparentar
que esos sistemas existen. Ver `FoundationAuditTest` y ejecutar `gradle check`
en ambos perfiles opcionales después de cambios de integración.
