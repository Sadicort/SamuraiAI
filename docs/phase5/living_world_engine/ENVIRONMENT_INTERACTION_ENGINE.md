# Environment Interaction Engine

**Código:** `living/world/environment/EnvironmentInteractionEngine.java`, `InteractionPoint.java` (motor puro); `living/server/InteractionBridge.java` (lado físico).

## Qué cubre

Puertas y dormir ya los resuelven la navegación (abrir puertas) y el scheduler (ir a la cama). Este motor añade lo que ellos no cubren, **solo a detalle completo (LOD 0)**:

- **Fuegos:** encender las hogueras (`CampfireBlock`) del asentamiento al atardecer y de noche, mantenerlas encendidas pasada la medianoche si hace frío (temperatura < `coldThreshold`) o hay fiesta, y apagarlas de madrugada.
- **Cultivos:** un campesino que está trabajando (rutina planificada WORK, ciudadano presente y con cuerpo) hace crecer un cultivo de la granja una etapa (`CropBlock`, propiedad `age`).
- **Camas:** las camas encontradas en una casa pasan a ser las camas reales de sus residentes (`HOME_ENGINE.md`).

## Puntos de interacción

`InteractionBridge` escanea **un edificio cada 20 ticks** (el que nunca se escaneó o el más antiguo, re-escaneo cada 12000 ticks), solo si su área está en chunks cargados, en una caja de radio ≤ 8 bloques, 3 por debajo y 4 por encima, con un máximo de 64 puntos por edificio. Clasifica: cama (cabecera) → `BED`, puerta (mitad inferior) → `DOOR`, hoguera → `FIRE`, cultivo → `CROP`, mesa de trabajo/yunque/horno/mesa de herrería/afilador/cortapiedras/telar/mesa de cartografía → `WORKSTATION`, cofre/barril → `STORAGE`, campana → `SHRINE`. `SEAT` y `WELL` existen en el modelo pero el escáner no los reconoce aún.

Los puntos **no se guardan**: el mundo es su fuente de verdad y se re-escanean tras reiniciar.

## Órdenes

Cada 100 ticks, para cada aldea cuya región está en `FULL`, `plan(asentamiento, fase del día, frío, festivo, campesinos trabajando, paso)` devuelve como mucho `maxInteractionOrders` (8) órdenes (`LIGHT_FIRE`, `DOUSE_FIRE`, `TEND_CROP`; `USE_WORKSTATION` está en el enum pero aún no se emite) y el puente las ejecuta comprobando que el bloque sigue siendo lo que se espera (una hoguera anegada no se enciende).

Interruptor: `worldInteractions` en `samuraiai-living.toml` (true). Cifras: `/samuraiai living metrics` («Interacciones: …»).

## Límites

- Sin jugadores cerca no hay nivel `FULL`, así que en el servidor de GameTest (sin jugadores) se prueban el escaneo y las camas, no el encendido de fuegos ni el cuidado de cultivos.
- No se colocan ni se rompen bloques; solo se cambian estados de bloques existentes.
