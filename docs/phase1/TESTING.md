# Pruebas y verificación

## Automatizado

`gradle check -PwithCustomNpcs=false` ejecuta 29 pruebas JUnit y un
GameTestServer. El resultado verificado el 15-09-2026 fue `BUILD SUCCESSFUL`,
29 tests pasando y `PHASE1_GAMETEST_OK customnpcs=false`. Las suites cubren
spawn/remove, lifecycle, diálogo ordenado, memoria, emociones, relaciones,
disponibilidad de Ollama, compatibilidad, hilos, timeout y cancelación.

El GameTest crea samurai, guard y merchant, comprueba índices, comandos, Brain,
remove y ausencia de NPC fantasma. `ClientSmoke` queda disponible para
`gradle runClient -PsmokeClient`.

## Perfiles

- Núcleo estable (CI recomendado): `gradle clean check -PwithCustomNpcs=false`.
- Integración externa: `gradle runGameTestServer -PwithCustomNpcs=true`.

El segundo perfil sólo es válido con una build CustomNPCs que no inicialice
clases cliente en servidor dedicado; el jar incluido falla en su propio
bootstrap, documentado en [CUSTOMNPCS_FIXES](CUSTOMNPCS_FIXES.md).
