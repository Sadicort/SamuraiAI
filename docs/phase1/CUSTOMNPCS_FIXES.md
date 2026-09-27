# Integración opcional con CustomNPCs

## Problemas

La dependencia tenía `versionRange` vacío y el refmap de Mixin no se remapeaba.
El núcleo también podía cargar clases `noppes.*` aunque el mod no estuviera
instalado.

## Solución

`mods.toml` declara `customnpcs`, `mandatory=false`,
`versionRange=[1.19.2.20250701]`, `ordering=AFTER` y `side=BOTH`. `build.gradle`
usa los stubs sólo para compilar y `runtimeOnly fg.deobf` únicamente en el perfil
explícito `-PwithCustomNpcs=true`. Los runs generan `output.srg` y remapean el
refmap; el log confirma `customnpcs.refmap.json` remapeado.

`IntegrationLoader` consulta `ModList` y selecciona una implementación por
reflexión. `CustomNPCsController` es la única clase productiva que importa
`noppes.*`; si falta, hay una versión incompatible o falla el enlace, se usa
`ChatNPCController` y el servidor continúa. El resto del núcleo sólo conoce la
interfaz `NPCController`.

## Verificación y limitación externa

El perfil sin el mod pasa Forge/GameTests: `PHASE1_GAMETEST_OK
customnpcs=false`. El perfil con el jar suministrado remapea correctamente, pero
el propio CustomNPCs 1.19.2.20250701 falla antes de SamuraiAI en servidor dedicado
al acceder a `net.minecraft.client.Minecraft` desde
`CustomNpcs.getLevelSaveDirectory` (`NullPointerException`). Es un defecto del
jar de terceros, no del adaptador; debe sustituirse por una build server-safe
antes de exigir el perfil con CustomNPCs en CI.

## Prueba

`CoreTest.integrationVersionIsDetected` valida detección. Ejecutar el núcleo con
`gradle check -PwithCustomNpcs=false`; probar el adaptador con
`gradle runGameTestServer -PwithCustomNpcs=true` después de instalar un jar
CustomNPCs corregido.
