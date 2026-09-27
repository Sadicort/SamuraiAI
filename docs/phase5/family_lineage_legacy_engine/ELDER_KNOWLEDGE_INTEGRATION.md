# Integración con el conocimiento de los ancianos

**Código:** `FamilyEngine.isVillageElder`, `FamilyEngine.elderTopics`.

## Sin un segundo motor de edad ni de conocimiento

Esta integración es **solo consulta**: no crea un sistema de edad nuevo (la Fase 3 ya tiene `LifeStage`/`Age Engine`, ver `AGE_ENGINE.md`), ni un sistema de conocimiento nuevo — cada línea que un anciano puede contar sale de datos que ya existían antes de esta extensión: la memoria familiar (`FamilyRecord.memory()`), los oficios por generación (`FamilyRecord.professionsByGeneration()`), las técnicas (`Technique`) y los linajes (`Lineage`). El motor de conocimiento cognitivo sigue siendo la única fuente de verdad para lo que un NPC sabe y puede enseñar; esto solo decide **quién** puede hablar de ello y **de qué**.

## Quién es el anciano de una aldea

```java
public boolean isVillageElder(UUID person) {
    // vivo, en etapa de vida ELDER, y nadie más vivo en su misma aldea es más viejo que él
}
```

No hay un cargo de "anciano del pueblo" asignado ni guardado en ningún sitio: se calcula al vuelo, comparando la edad (`FamilyEngine.age`, que ya viene del calendario) de todos los residentes vivos de la misma aldea (`Villages.villageOf`, uno de los puertos de `FamilyPorts`). Si dos ancianos empatan en edad exacta, ninguno de los dos verá `false` a un rival estrictamente mayor — el primero que se consulte con esa edad se considera el anciano; no hay un desempate explícito porque el calendario no suele producir edades idénticas al minuto.

## De qué puede hablar

`elderTopics(person)` reúne, sin inventar nada nuevo:

| Fuente | Qué aporta |
| --- | --- |
| `FamilyRecord.memory()` | guerras, traiciones y heroísmos (`FamilyMemoryEntry.Kind.WAR/BETRAYAL/HEROISM`) con significancia ≥ `storySignificance`, con el año en que ocurrieron |
| `FamilyRecord.professionsByGeneration()` | oficios que la familia ha practicado a través de generaciones |
| `Technique` | técnicas que la persona posee o creó (marcando si ya se perdió) |
| `Lineage` | escuelas de más de una generación de las que forma parte |

Ninguna de estas listas depende de que la persona sea el anciano de su aldea: `elderTopics` es una consulta abierta sobre lo que **esa persona en concreto** sabe por su propia familia y su propio linaje; `isVillageElder` es la comprobación aparte de si, además, tiene la autoridad social de ser quien lleva la voz de la memoria del pueblo.

## Por qué no todo anciano es automáticamente "sabio"

La especificación prohíbe explícitamente marcar a todo anciano como sabio por defecto. Ni `isVillageElder` ni `elderTopics` conceden ningún epíteto, honor ni reputación por sí solos — ser el anciano de la aldea no otorga nada mágico; si esa persona ha ganado honor de verdad, ya lo reflejará su propio epíteto (`EPITHET_ENGINE.md`) por las causas normales, no por su edad.

## Pruebas

`FamilyIdentityExtensionTest.theOldestLivingResidentOfAVillageIsItsElderAndCanSpeakToRealFamilyHistory`: crea una familia con una guerra real registrada en su memoria, confirma que el residente vivo de más edad de la aldea es su anciano y que `elderTopics` incluye esa guerra.
