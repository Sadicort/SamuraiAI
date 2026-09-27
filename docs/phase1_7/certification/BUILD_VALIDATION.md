# Build Validation

Validación ejecutada el 2026-09-19:

- `verifyDistributionJar`: PASS.
- Dos ejecuciones `clean verifyDistributionJar`: PASS.
- Tamaño en ambas: 711.353 bytes.
- SHA-256 en ambas:
  `23FE71845C2BB68635AB877CD775454B8CF358D12EBAA1B0C76BD31398EBEBD8`.

Esto demuestra reproducibilidad local con las mismas entradas. No sustituye la
prueba JAR-only en una instalación Forge limpia ni una firma de publicación.

El gate verifica mixin, descriptor de auditores, Jar-in-Jar de Whisper, DLL de
Windows, ausencia de stubs/tests/audio y reofuscación SRG. No instala ni publica
el JAR. Falta una prueba de instalación limpia usando exclusivamente el JAR.
