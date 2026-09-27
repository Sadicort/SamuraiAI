# Release Pipeline

No hay publicación automática autorizada. Un release futuro debe generar el
JAR, ejecutar las matrices, calcular SHA-256, reunir evidencia contra ese hash y
solo entonces invocar `FoundationValidator` y publicar su certificado.
