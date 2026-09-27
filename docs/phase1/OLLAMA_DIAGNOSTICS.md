# Ollama: disponibilidad y fallbacks

`OllamaDiagnostics` consulta `/api/tags`, valida host, puerto, timeout y modelo y
devuelve estados `AVAILABLE`, `UNAVAILABLE`, `MODEL_NOT_FOUND`, `HTTP_ERROR` o
`JSON_ERROR`. El servidor ejecuta el chequeo al arrancar en segundo plano y sólo
programa el log en el hilo principal.

`OllamaAIService` limita prompt e historial, limpia la respuesta y usa
`AIError`/`AIResponse` para garantizar texto alternativo ante timeout, modelo
ausente, Ollama apagado, cola llena, HTTP o JSON inválido. Nunca entrega el
resultado directamente a una entidad.

`OllamaDiagnosticsTest` usa un `HttpServer` local para cubrir disponibilidad,
modelo incorrecto, timeout, HTTP y JSON. Para diagnóstico manual: configurar
`samuraiai-client.toml` y ejecutar `/samurai probe` en un servidor activo.
