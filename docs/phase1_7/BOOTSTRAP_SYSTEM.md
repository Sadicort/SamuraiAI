# Bootstrap Foundation

## Flujo implementado

1. `Samuraiai` registra configuración común; `ClientBootstrap` registra voz solo
   en la distribución física cliente.
2. `FoundationBootstrap.setup` ejecuta auditorías pequeñas durante common setup.
3. El reporte inmutable queda disponible antes del inicio de runtime.
4. `Samuraiai.starting` consulta permiso del núcleo antes de activar scheduler,
   cola de IA, integración y restauración existentes.
5. `VoiceBootstrapService.start` consulta permiso del módulo Voice. Su instalación
   y carga nativa se ejecutan en el worker de voz, no en el hilo de render.

No se duplicaron Brain ni Dialogue. La regla de parada fue aclarada por el
usuario: las reparaciones están permitidas, pero no avanzar/certificar con
fallos críticos sin resolver.

## Riesgos y pruebas

Common setup no realiza benchmark, descarga ni inferencia. El reporte usa I/O
pequeño en el worker de setup; futuras verificaciones costosas necesitan tareas
acotadas independientes. El Module Loader completo todavía no existe: las
decisiones se conectaron a los puntos de entrada existentes.

Ejecutar GameTests con/sin CustomNPCs, revisar `startupAllowed` y confirmar que
un BLOCKER de auditoría impide iniciar el runtime de SamuraiAI sin cerrar Forge.
La última condición se prueba a nivel de política en JUnit; falta inyección de
fallos de empaquetado en una ejecución Forge dedicada para verificarla de punta
a punta.
