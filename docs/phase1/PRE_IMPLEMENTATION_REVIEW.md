# Fase 1: revisión previa
Fecha: 2026-09-15.

Se revisaron los 99 archivos de código propio, paquetes, controladores, Brain, runtime, eventos, modelos HTTP, configuración, metadata y referencias de integración. La revisión previa detallada y las once imágenes están en [el análisis completo](../../ANALISIS_COMPLETO_PROYECTO.md).

## Decisiones de implementación
- Mantener Brain → Behavior → Task → Action → Controller.
- Scheduler con sesión invalidable y ejecución exclusiva del servidor.
- Cola global FIFO acotada, más conversaciones serializadas por NPC.
- Cancelación propagada hasta el future HTTP y validación del runtime antes de entregar.
- Integración noppes exclusivamente bajo integration/customnpcs y carga condicional.
- Servicios para emociones y relaciones, índices y lifecycle validado.
- Persistencia: DTO versionado y hooks, sin escritura automática de estado duradero.
- JUnit y GameTests con resultados reales en [VERIFICATION.md](VERIFICATION.md).
- Sin movimiento, patrulla, seguimiento, comercio o acciones de combate en esta fase.

## Evidencia de remapeo
El jar incluye customnpcs.mixins.json y customnpcs.refmap.json. El accessor removalReason se traduce a f_146795_. El log histórico presenta ese nombre contra Minecraft con mappings oficiales. Se debe remapear el refmap en los runs de desarrollo, conservando el jar de terceros intacto.

Referencias técnicas primarias:
- [Opciones de Mixin](https://github.com/SpongePowered/Mixin/blob/master/src/main/java/org/spongepowered/asm/mixin/MixinEnvironment.java).
- [Configuración MixinGradle del refmap](https://github.com/SpongePowered/MixinGradle/blob/master/src/main/groovy/org/spongepowered/asm/gradle/plugins/MixinExtension.groovy).
- [Runs ForgeGradle](https://docs.minecraftforge.net/en/fg-5.x/configuration/runs/).

## Límite de admisión
Una cola finita no puede aceptar una cantidad infinita de solicitudes. Saturar los trabajadores hará esperar las solicitudes; superar también la capacidad de espera producirá un resultado explícito QUEUE_FULL, con fallback. Nunca se bloquea el hilo del servidor esperando un cupo.
