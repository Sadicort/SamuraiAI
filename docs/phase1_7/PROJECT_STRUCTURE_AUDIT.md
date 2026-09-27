# Auditoría de estructura

`StructureAudit` comprueba recursos `.class` de las responsabilidades principales,
sin inicializarlas. Reutiliza rutas actuales: diálogo y scheduler en `runtime`,
relaciones en `npc.relationship`, eventos en `event` y voz bajo `client.voice`.
Esto evita crear paquetes vacíos solo para satisfacer la lista conceptual de
la guía. La ausencia de una clase obligatoria comprobada genera BLOCKER.

Voice se valida separadamente en cliente. Debug y herramientas de desarrollo
son opcionales; no se convierten artificialmente en dependencias de un servidor.

`ResourceAudit` verifica presencia de metadata de arranque; el control del
contenido del JAR se ejecuta en `verifyDistributionJar`. Los checks actuales no
demuestran por sí solos que todas las clases tienen responsabilidades correctas
o que no existen referencias transitivas incorrectas.

Pruebas: discovery/registro real en `FoundationAuditTest`, compilación y gate
de empaquetado en `gradle check`. Pendientes: inventario semántico exhaustivo,
validación de todos los recursos y análisis de bytecode por lado.
