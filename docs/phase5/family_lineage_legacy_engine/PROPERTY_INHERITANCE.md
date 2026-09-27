# Property Inheritance

**Código:** `living/family/inheritance/InheritanceRecord.java`; `FamilyEngine.inheritFrom/execute`.

Al morir alguien (muerte real en el mundo):

1. **Herederos** por prioridad: parejas vivas, luego hijos vivos del mayor al menor; si no hay, hermanos; si no, el jefe de la familia.
2. **Bienes:** monedas de su cuenta (Economy), edificios de los que es dueño (Village), reliquias que lleva (registro de reliquias).
3. **Ejecución** en los motores dueños: las monedas se reparten a partes iguales por transferencia (nunca se crean); los edificios pasan al primer heredero (`transferBuilding`); las reliquias también. Tipos preparados sin mecanismo aún (tierra, herramientas, armas, negocio, recursos) quedan anotados como «sin mecanismo aún».
4. Estado `EXECUTED`, `PARTIAL` o `VOID` (sin herederos); memoria familiar («La herencia de … pasa a …»); `InheritanceCompletedEvent`.

El registro es la **historia de propiedad**; la propiedad en sí se mueve en el motor que la posee.
