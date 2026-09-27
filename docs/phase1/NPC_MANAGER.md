# NPCManager

`NPCManager` ahora mantiene un registro sincronizado con índices por UUID,
nombre, tipo, estado y mundo. El nombre solicitado se sanea y recibe sufijos
deterministas (`_2`, `_3`, …) cuando está ocupado. Registrar dos veces el mismo
UUID o eliminar dos veces produce una operación segura y un log explícito.

Todas las mutaciones requieren el hilo del servidor; las lecturas devuelven
copias inmutables o snapshots. `refresh` mantiene los índices cuando cambia el
estado, mundo o nombre. Las pruebas cubren búsqueda, tipos, estados, mundos,
nombres únicos y eliminación.

Riesgo futuro: si se añaden índices geográficos deberán actualizarse dentro de la
misma sección sincronizada y liberarse desde `unregister`.
