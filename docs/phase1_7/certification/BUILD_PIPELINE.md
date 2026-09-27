# Build Pipeline

Los `AbstractArchiveTask` usan orden reproducible y timestamps normalizados.
Esto es obligatorio porque la evidencia Foundation se liga al SHA-256 exacto
del JAR. La comprobación práctica consiste en ejecutar dos builds limpios con
las mismas entradas y comparar el hash de `samuraiai-1.0-SNAPSHOT.jar`.

`gradle check` ejecuta JUnit, GameTest dedicado y `verifyDistributionJar`.
Los perfiles con y sin CustomNPCs se ejecutan separadamente. El cliente Whisper
se valida mediante `runClient -PsmokeClient -PvoiceSmokeFixture=...`.
