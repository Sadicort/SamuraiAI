# Verificación reproducible de Fase 1

Fecha de ejecución: 2026-09-15. Entorno: Windows 11, Java 17.0.12, Minecraft
1.19.2, Forge 43.5.2, Gradle 8.4.

## Resultado principal

Comando:

```text
gradle clean check
```

El perfil por defecto es `withCustomNpcs=false` para que CI no dependa del jar
externo. Resultado: `BUILD SUCCESSFUL`.

- 29 pruebas JUnit pasaron (incluyen las comprobaciones de behaviors de Fase 3).
- Forge GameTestServer inició y se detuvo correctamente.
- `PHASE1_GAMETEST_OK customnpcs=false`.
- Se crearon y eliminaron samurai, guard y merchant.
- Se verificaron Brain, lifecycle, índices, comandos y ausencia de cuerpo/referencia
  tras remove.
- El startup check de Ollama informó `AVAILABLE` con `llama3.2:latest`.
- El log de Mixin mostró remapeo de `customnpcs.refmap.json` mediante `output.srg`.
- Las acciones/behaviors y `SavedData` de las fases posteriores compilan con
  `gradle compileJava` y el `check` completo sigue pasando.

## Matriz de escenarios

| Escenario | Resultado |
|---|---|
| Forge/GameTest sin CustomNPCs | PASA |
| Ollama disponible | PASA |
| Ollama apagado/HTTP/JSON/modelo incorrecto | JUnit PASA (servidor local simulado) |
| Cola FIFO, saturación, timeout y cancelación | JUnit PASA |
| Spawn/remove/lifecycle/memoria | JUnit + GameTest PASA |
| Callback fuera del hilo principal | JUnit PASA |
| Forge cliente smoke | Preparado, ejecutar `gradle runClient -PsmokeClient` |
| Forge con CustomNPCs suministrado | BLOQUEADO por fallo del jar externo |

## Bloqueo externo

Con `-PwithCustomNpcs=true`, el refmap se remapea pero
`CustomNpcs.getLevelSaveDirectory` del jar
`CustomNPCs-1.19.2-GBPort-Unofficial-20250701.jar` carga
`net.minecraft.client.Minecraft` en dedicado y produce `NullPointerException`.
SamuraiAI no puede corregir una clase de terceros sin reemplazar ese artefacto;
la integración queda lista para una build server-safe.
