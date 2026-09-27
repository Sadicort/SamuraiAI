# Resource Manager

## Problema

Cada subsistema administraba rutas propias y no existía una frontera común que
impidiera escapes de directorio o instalaciones no atómicas.

## Implementación

`FoundationResourceManager` posee config, voice, cache, logs, diagnostics,
reports, temp, downloads y assets. Normaliza rutas, rechaza `..`, crea las
carpetas en bootstrap, calcula SHA-256, valida tamaño+hash e instala mediante un
staging en el mismo directorio y `ATOMIC_MOVE`. La limpieza solo recorre archivos
de temp/downloads administrados; nunca carpetas amplias o datos ajenos.

Voice conserva sus managers especializados. El Resource Manager no suplanta el
catálogo de modelos ni convierte un hash en autenticidad criptográfica.

## Pruebas y riesgos

`FoundationResourceManagerTest` cubre preparación, traversal, reemplazo válido,
preservación del destino ante corrupción y limpieza acotada. Falta definir el
origen y la clave pública para actualizaciones firmadas; no se ha inventado una
raíz de confianza.
