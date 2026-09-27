# Dependencias y empaquetado

## Fallos reproducidos

El primer smoke real instaló correctamente el modelo, pero no pudo cargar
`WhisperCpp`: una dependencia `implementation` no se expone automáticamente
al classloader del juego Forge en desarrollo. Al pasarla a `minecraftLibrary`,
su JNA transitivo 5.13.0 entró en conflicto con JNA 5.12.1 de Minecraft.
Además, el JAR con dependencias embebidas no estaba conectado a reofuscación.

## Reparaciones de build.gradle

- `minecraftLibrary` para Whisper 1.4.0 durante desarrollo.
- `jarJar` para incluir el binding y las bibliotecas nativas en el artefacto.
- Ambas declaraciones excluyen transitivas: se reutiliza JNA de Minecraft.
- `reobfJarJar` se ejecuta al generar el artefacto con dependencias.
- `MixinConfigs` se incluye en los manifests.
- `verifyDistributionJar`, incluido en `check`, comprueba metadata, binding,
  DLL Windows, ausencia de JNA duplicada, stubs, GameTests y WAV, y referencia
  SRG del adaptador de directorios.

Referencia: [dependencias no Minecraft en ForgeGradle 6](https://docs.minecraftforge.net/en/fg-6.x/dependencies/).

## Resultado y límites

Whisper transcribió en cliente Forge con y sin CustomNPCs. El control estructural
del JAR pasó. Es evidencia de compatibilidad con JNA de este entorno, no garantía
universal. Todavía falta ejecutar una instalación Forge de producción que cargue
solo el JAR reofuscado y sus dependencias opcionales.

`DependencyAudit` valida Java 17, Minecraft 1.19.2 y familia Forge 43 al iniciar.
No instala JUnit dentro del mod ni añade Jackson: se conserva Gson existente.
La revisión de licencias/SBOM, vulnerabilidades y locking completo permanece
pendiente. Generar este artefacto para pruebas no constituye publicación ni
certificación de Foundation Complete.
