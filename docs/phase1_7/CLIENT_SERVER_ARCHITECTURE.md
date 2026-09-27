# Separación cliente/servidor

## Problema y reparación

`Samuraiai` registraba directamente clases de configuración situadas en el
paquete cliente. Ahora delega a `client.ClientBootstrap` mediante
`DistExecutor` únicamente en distribución CLIENT. El common bootstrap no
importa GUI, audio ni motores nativos.

`FoundationBootstrap` usa un enum propio para la distribución física. Los
auditores consultan recursos sin inicializar clases opcionales. En dedicado,
`VoiceResourceAudit` devuelve NOT_APPLICABLE antes de consultar el binding.
`VoiceBootstrapService` y `IntegrationLoader` respetan las decisiones del auditor.

La distribución física no sustituye los controles de hilo lógico: singleplayer
ejecuta servidor y cliente dentro del mismo proceso. La lógica del servidor
continúa en `ServerScheduler`. Referencia:
[Forge: lados físicos y lógicos](https://docs.minecraftforge.net/en/1.19.x/concepts/sides/).

## Pruebas y límites

Probar `gradle check`, `gradle check '-PwithCustomNpcs=true'` y cliente con ambos
perfiles. `FoundationAuditTest` comprueba que dedicado no inspecciona Voice.
El análisis exhaustivo de referencias transitivas/bytecode todavía no está
implementado; la etapa SIDES permanece NOT_RUN en el informe automático.
