# Scheduler y presupuesto de tick

Los deadlines globales se ejecutan en AsyncEngine y nunca llaman Minecraft.
`ServerScheduler` sigue siendo la frontera de retorno al hilo servidor y descarta
tareas de una sesión anterior.

`TickBudget` limita por cantidad y por 4 ms el trabajo Brain de cada tick.
`NPCTickService` calcula la cuota según NPC activos/intervalo, difiere el resto y
publica el último snapshot para diagnóstico. `TickBudgetTest` cubre agotamiento
por tiempo y cantidad. La prueba de 500 runtimes es lógica, no un benchmark TPS
con 500 entidades físicas.
