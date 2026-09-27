# Fase 1.7 — Reparación de arranque dedicado con CustomNPCs

## Problema

`CustomNpcs.getLevelSaveDirectory(String)` de la versión `1.19.2.20250701`
llamaba a `Minecraft.getInstance()` cuando el servidor no era singleplayer.
El directorio calculado mediante esa llamada ni siquiera se utilizaba. En
dedicado, Forge rechazaba cargar la clase cliente; después se recibía `null`
y `PlayerDataController` fallaba al enumerar archivos.

## Solución y clases modificadas

- `integration/customnpcs/mixin/CustomNPCsSaveDirectoryMixin`: intercepta solo
  el método afectado, antes de su ejecución, en distribución SERVER.
- `CustomNPCsMixinPlugin`: selecciona únicamente el mod instalado y la versión
  exacta afectada, usando metadatos del loader, sin cargar `noppes.*`.
- `CustomNPCsSaveDirectory`: obtiene la ruta del servidor actual mediante
  `ServerLifecycleHooks` y `LevelResource`, sin referencias al cliente.
- `samuraiai.customnpcs.mixins.json` y `build.gradle`: registran la corrección en
  desarrollo y en los manifests de ambos artefactos JAR.

No se modificó ni reemplazó el JAR de CustomNPCs. No se añadió un segundo
controlador. El target de terceros no está ofuscado: utiliza `remap=false`;
las llamadas a Minecraft se mantienen en una clase Java normal que Forge puede
reofuscar. No se inventa un refmap para un método que no lo necesita.
Referencia técnica: [anotación Mixin de SpongePowered](https://github.com/SpongePowered/Mixin/blob/master/src/main/java/org/spongepowered/asm/mixin/Mixin.java).

## Prueba ejecutada

```powershell
gradle runGameTestServer '-PwithCustomNpcs=true' --stacktrace
```

El 2026-09-18, ejecución iniciada a las 16:05 AST: `BUILD SUCCESSFUL`.
Forge inició el servidor, seleccionó el adaptador de CustomNPCs y pasó el
GameTest existente de spawn/remove, lifecycle ACTIVE, índices, memoria y
presencia/eliminación del cuerpo para samurai, guard y merchant.

Marcadores: `PHASE1_GAMETEST_OK customnpcs=true` y
`All 1 required tests passed`. Evidencia:
`build/phase1/with-customnpcs/logs/latest.log`.

## Riesgos y verificaciones restantes

La prueba es del entorno de desarrollo, no una certificación del JAR distribuido.
La compatibilidad de otras versiones no está acreditada. Se mantiene el aviso
de recurso externo `Failed to get filter section from pack customnpcs`, que no
impidió el GameTest. No se declara «sin errores» al mod externo completo.

Se debe repetir el perfil sin CustomNPCs, probar cliente/singleplayer y el
artefacto reofuscado. Si cambia la firma del método de terceros, la inyección
debe fallar de forma visible, no dejar una supuesta reparación aplicada.
