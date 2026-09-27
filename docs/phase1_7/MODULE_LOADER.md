# Module Loader

## Problema

Foundation solo podía deshabilitar módulos desde el informe de auditoría. No
existían descriptores, resolución de dependencias, estados ni rollback.

## Implementación

`foundation.module` incorpora `ModuleLoader`, descriptor inmutable, dependencias
requeridas/opcionales, lados, prioridad, estados y snapshots. La carga es
determinista y topológica. Detecta ciclos, dependencias ausentes y fallos de
inicialización; un fallo revierte en orden inverso lo abierto en la transacción.
Un módulo CLIENT nunca ejecuta su inicializador en servidor dedicado.

`FoundationModules` registra Common, Client, Brain, Dialogue, Ollama, Voice,
Debug, Developer Tools y CustomNPCs cuando está presente. El bootstrap publica
los módulos READY en el contexto del auditor y `ModuleAudit` valida los módulos
obligatorios. `FoundationBootstrap.allowed` combina auditoría y estado modular.

## Pruebas

`ModuleLoaderTest` cubre orden, cierre inverso, ciclos, rollback, lados y
dependencias ausentes. Los GameTests verificaron el grafo dentro de Forge con y
sin CustomNPCs. Riesgo futuro: los módulos de gameplay añadidos en Fase 2 deben
registrar inicializadores reales y evitar dependencias circulares.
