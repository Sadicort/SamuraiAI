# Actualizaciones y rollback

`VoiceDownloadManager` descarga con `HttpClient` a un archivo `.part` y sólo
renombra al destino tras recibir HTTP 2xx. `VoiceIntegrityChecker` valida el
resultado antes de activarlo; el modelo anterior no se toca durante la descarga.

El manifest actual instala el modelo inicial oficial. Las futuras versiones
deben cambiar versión, tamaño y SHA-256 en el manifest; una descarga inválida
se elimina y nunca reemplaza un modelo funcional.
