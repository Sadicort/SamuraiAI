# Modo offline

El bootstrap no depende de Ollama ni de Internet para la arquitectura. La
primera ejecución descarga y verifica el modelo; las siguientes cargan el
archivo local y el runtime embebido sin red.

Si la descarga o el hash falla, el engine queda en `ERROR` y muestra un
diagnóstico controlado. Nunca produce texto falso ni inicia procesos externos.
