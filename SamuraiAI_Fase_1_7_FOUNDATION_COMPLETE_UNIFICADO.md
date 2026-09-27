# SamuraiAI — Fase 1.7: FOUNDATION COMPLETE
## Documento Maestro Unificado (Partes 1, 2, 3, 4A y 4B)

**Proyecto:** SamuraiAI
**Versión del Proyecto:** Foundation v1.7
**Minecraft:** Forge 1.19.2
**Java:** Java 17
**Arquitectura:** Modular AI Engine
**Integración:** CustomNPCs-Unofficial + Ollama + Voice Core
**Prioridad:** CRÍTICA (Bloquea todas las fases posteriores)

> Este documento une, sin recortes, las cinco partes originales de la Fase 1.7 — Foundation Complete — en un único documento maestro de ingeniería para Codex:
> **Parte 1** (Auditoría y Arquitectura Cliente/Servidor) · **Parte 2** (Voice Core, Async Engine y Thread Safety) · **Parte 3** (NPC Lifecycle, EventBus y Runtime Engine) · **Parte 4A** (Testing Framework 2.0 y Compatibility Suite) · **Parte 4B** (Performance Suite 2.0, Benchmark Engine, Release Pipeline y Foundation Certificate).

---

## Índice General

### PARTE 1 — Foundation Audit Engine, Arquitectura Cliente/Servidor y CustomNPCs Compatibility
- Prólogo — El nacimiento de la fundación de SamuraiAI
- Capítulo 1 — Reglas absolutas de ingeniería de SamuraiAI
- Capítulo 2 — Foundation Audit Engine
- Capítulo 3 — Project Structure Audit
- Capítulo 4 — Dependency Audit
- Capítulo 5 — Client / Server Architecture Audit
- Capítulo 6 — Sided Bootstrap System
- Capítulo 7 — Side Guard System
- Capítulo 8 — CustomNPCs Compatibility Layer
- Capítulo 9 — Module Loader
- Capítulo 10 — Module Metadata System
- Capítulo 11 — Resource Manager
- Capítulo 12 — Startup Diagnostics Engine
- Capítulo 13 — Foundation Report Generator
- Capítulo 14 — Documentación Automática
- Capítulo 15 — Checklist de Implementación (Parte 1)

### PARTE 2 — Voice Core Completion, Async Engine y Thread Safety
- Capítulo 16 — Voice Core Completion
- Capítulo 17 — Model Manager Profesional
- Capítulo 18 — Download Manager
- Capítulo 19 — Voice Configuration Center
- Capítulo 20 — Audio Device Manager
- Capítulo 21 — Audio Pipeline Profesional
- Capítulo 22 — Voice Threading Engine
- Capítulo 23 — Thread Safety Audit
- Capítulo 24 — Global Async Engine
- Capítulo 25 — AI Request Queue Profesional
- Capítulo 26 — Task Scheduler Profesional
- Capítulo 27 — Foundation Thread Report
- Capítulo 28 — Documentación Obligatoria (Parte 2)
- Capítulo 29 — Checklist de Implementación (Parte 2)

### PARTE 3 — NPC Lifecycle, EventBus 2.0, Runtime Engine y Developer Toolkit
- Capítulo 30 — El Lifecycle definitivo de los NPC
- Capítulo 31 — Runtime Manager Completo
- Capítulo 32 — NPC Removal Pipeline
- Capítulo 33 — NPC Activation / Deactivation Engine
- Capítulo 34 — Memory Cleanup Engine
- Capítulo 35 — EventBus 2.0
- Capítulo 36 — Catálogo Completo de Eventos
- Capítulo 37 — Event Diagnostics
- Capítulo 38 — Runtime Diagnostics Center
- Capítulo 39 — Performance Budget System
- Capítulo 40 — Logger 2.0
- Capítulo 41 — Debug Console
- Capítulo 42 — Developer Toolkit
- Capítulo 43 — Config Validation Engine
- Capítulo 44 — Resource Validation Engine
- Capítulo 45 — Foundation Snapshot System
- Capítulo 46 — Documentación Automática (Parte 3)
- Capítulo 47 — Checklist de Implementación (Parte 3)

### PARTE 4A — Testing Framework 2.0, Compatibility Suite y QA Automation Framework
- Prólogo — La certificación del núcleo
- Capítulo 48 — Testing Framework 2.0
- Capítulo 49 — Test Framework Core
- Capítulo 50 — Discovery Engine
- Capítulo 51 — Test Execution Engine
- Capítulo 52 — JUnit Core Suite
- Capítulo 53 — Forge GameTest Suite
- Capítulo 54 — Integration Test Framework
- Capítulo 55 — Regression Test Engine
- Capítulo 56 — Stress Test Framework
- Capítulo 57 — Compatibility Suite
- Capítulo 58 — CustomNPCs Compatibility Matrix
- Capítulo 59 — Voice Compatibility Matrix
- Capítulo 60 — Ollama Compatibility Suite
- Capítulo 61 — Config Validation Tests
- Capítulo 62 — Resource Validation Tests
- Capítulo 63 — QA Automation Framework
- Capítulo 64 — Test Report Generator
- Capítulo 65 — QA Validation Checklist
- Capítulo 66 — Resultado Esperado de la Parte 4A

### PARTE 4B — Performance Suite 2.0, Benchmark Engine, Release Pipeline y Foundation Certificate
- Prólogo — La certificación definitiva del núcleo
- Capítulo 73 — Performance Suite 2.0
- Capítulo 74 — CPU Monitor
- Capítulo 75 — Memory Monitor
- Capítulo 76 — Thread Monitor
- Capítulo 77 — TPS & Tick Analyzer
- Capítulo 78 — Benchmark Engine
- Capítulo 79 — Benchmark Suites
- Capítulo 80 — Historical Performance Database
- Capítulo 81 — Compatibility Certification Suite
- Capítulo 82 — Regression Certification
- Capítulo 83 — Release Pipeline
- Capítulo 84 — Build Validation Engine
- Capítulo 85 — Foundation Certificate Engine
- Capítulo 86 — Foundation Certificate
- Capítulo 87 — Foundation Status JSON
- Capítulo 88 — Release Checklist
- Capítulo 89 — Documentación Automática (Parte 4B)
- Capítulo 90 — Criterios para desbloquear la Fase 2
- Epílogo — Foundation Complete

> **Nota sobre numeración:** los capítulos 67 a 72 no aparecen en los documentos originales entregados (la Parte 4B continúa directamente en el Capítulo 73); se conserva la numeración original tal como fue redactada para no alterar las referencias cruzadas del documento maestro.

---



---

# =========================================================
# PARTE 1 — FOUNDATION AUDIT ENGINE, ARQUITECTURA CLIENTE/SERVIDOR Y CUSTOMNPCS COMPATIBILITY
# =========================================================

# SamuraiAI — Fase 1.7: FOUNDATION COMPLETE

## Documento Maestro de Ingeniería para Codex

**Proyecto:** SamuraiAI

**Versión del Proyecto:** Foundation v1.7

**Minecraft:** Forge 1.19.2

**Java:** Java 17

**Arquitectura:** Modular AI Engine

**Integración:** CustomNPCs-Unofficial + Ollama + Voice Core

**Prioridad:** CRÍTICA (Bloquea todas las fases posteriores)

---

# PRÓLOGO — EL NACIMIENTO DE LA FUNDACIÓN DE SAMURAIAI

## Este documento cambia completamente el proyecto.

Hasta este punto SamuraiAI es un prototipo extremadamente prometedor. Posee un Brain, Runtime, Dialogue Router, AI Queue, Voice Core inicial y una arquitectura modular bien diseñada. Sin embargo, el análisis técnico encontró varios problemas estructurales que impiden comenzar el verdadero desarrollo del motor de inteligencia artificial.

Esta fase existe por una única razón:

**Convertir SamuraiAI en una plataforma estable de desarrollo.**

No estamos agregando patrullas.

No estamos agregando combate.

No estamos agregando memoria persistente.

No estamos agregando nuevas habilidades.

Estamos construyendo la fundación sobre la que absolutamente todo el proyecto dependerá.

Una vez completada esta fase, SamuraiAI deberá poder evolucionar durante años sin volver a modificar su núcleo.

Este documento debe considerarse la constitución técnica del proyecto.

---

# CAPÍTULO 1 — REGLAS ABSOLUTAS DE INGENIERÍA DE SAMURAIAI

Estas reglas tienen prioridad sobre cualquier otra instrucción futura.

## Regla 1 — Nunca romper la arquitectura existente

Antes de escribir una sola línea de código debes analizar completamente el proyecto.

Analizar:

* paquetes
* clases
* interfaces
* eventos
* runtime
* brain
* scheduler
* controllers
* integración
* config
* tests
* recursos

No reemplazar sistemas funcionales.

Solo extenderlos o corregirlos.

---

## Regla 2 — Auditoría obligatoria antes de programar

Cada fase comienza ejecutando una auditoría automática.

La auditoría debe revisar:

Arquitectura.

Dependencias.

Errores.

Duplicados.

Concurrencia.

Eventos.

Inicialización.

Recursos.

Configuración.

Cliente.

Servidor.

Si falla cualquier verificación crítica, detener inmediatamente la implementación.

Nunca continuar.

---

## Regla 3 — Documentación obligatoria

Cada cambio genera documentación.

Crear automáticamente archivos Markdown.

Ningún sistema puede implementarse sin documentación.

Cada documento debe explicar:

Objetivo.

Arquitectura.

Clases.

Responsabilidades.

Flujos.

Errores comunes.

Pruebas.

Extensión futura.

---

## Regla 4 — Todo módulo debe ser desacoplado

Brain jamás conoce Minecraft.

Minecraft jamás conoce Brain.

Controllers traducen.

Adapters integran.

Events comunican.

Nunca crear dependencias circulares.

---

## Regla 5 — Cliente y servidor son mundos distintos

Ninguna clase del cliente puede cargarse en servidor dedicado.

Ninguna referencia de GUI puede existir en Runtime.

Toda lógica visual vive únicamente en el cliente.

---

## Regla 6 — Ningún hilo externo modifica Minecraft

Toda interacción con entidades debe ejecutarse únicamente en el hilo principal del servidor o cliente correspondiente.

Ollama.

Voice.

HTTP.

IO.

Nunca modifican entidades directamente.

---

## Regla 7 — Todo comportamiento debe ser verificable

Cada sistema implementado debe incluir:

Logs.

Tests.

Diagnósticos.

Documentación.

No existen sistemas "ocultos".

---

## Regla 8 — SamuraiAI debe poder funcionar sin módulos opcionales

Módulos opcionales:

CustomNPCs.

Voice.

Ollama.

Debug Overlay.

Developer Toolkit.

El proyecto debe iniciar incluso si alguno falta.

---

# CAPÍTULO 2 — FOUNDATION AUDIT ENGINE

## Objetivo

Crear el primer sistema inteligente del proyecto.

No es IA.

Es un auditor automático.

Antes de iniciar SamuraiAI debe revisar completamente el estado interno del proyecto.

El auditor se convierte en el guardián del núcleo.

---

## Arquitectura del Foundation Audit Engine

Crear un paquete completamente nuevo.

```text
yadi.samuraiai.foundation.audit

audit/
├── FoundationAuditEngine.java
├── AuditPipeline.java
├── AuditStage.java
├── AuditRegistry.java
├── AuditContext.java
├── AuditSeverity.java
├── AuditResult.java
├── AuditReporter.java
├── AuditFixSuggestion.java
├── AuditSummary.java
├── AuditTimer.java
└── AuditMetrics.java
```

Toda auditoría pasa por este sistema.

Nunca realizar verificaciones manuales dispersas.

---

## Flujo completo del auditor

Minecraft inicia.

↓

FoundationBootstrap.

↓

FoundationAuditEngine.

↓

AuditRegistry.

↓

Ejecutar todas las etapas.

↓

Generar resultados.

↓

Generar reporte.

↓

Permitir o bloquear SamuraiAI.

---

## Audit Pipeline

La auditoría se divide en etapas.

Orden obligatorio.

### Etapa 1

Project Structure Audit.

### Etapa 2

Dependency Audit.

### Etapa 3

Client Server Audit.

### Etapa 4

Configuration Audit.

### Etapa 5

Resource Audit.

### Etapa 6

Module Audit.

### Etapa 7

Thread Audit.

### Etapa 8

Compatibility Audit.

### Etapa 9

Performance Audit.

### Etapa 10

Foundation Validation.

---

## Audit Registry

El Registry registra automáticamente todas las auditorías disponibles.

No hardcodear auditorías.

Cada auditor implementa una interfaz común.

Preparado para futuras fases.

---

## Audit Context

El contexto contiene toda la información del proyecto.

Versiones.

Forge.

Mods.

Configuración.

Entorno.

Sistema operativo.

Cliente.

Servidor.

Recursos.

Módulos.

Todo disponible para cualquier auditor.

---

## Severidad

Cinco niveles.

INFO

WARNING

ERROR

CRITICAL

BLOCKER

BLOCKER detiene SamuraiAI.

CRITICAL deshabilita módulos afectados.

ERROR registra y continúa.

---

# CAPÍTULO 3 — PROJECT STRUCTURE AUDIT

## Objetivo

Verificar absolutamente toda la estructura del proyecto.

---

## Verificaciones obligatorias

### Java Packages

Verificar existencia.

brain

runtime

behavior

voice

dialogue

integration

events

memory

emotion

relationship

npc

scheduler

controller

config

debug

foundation

Si falta alguno obligatorio.

Generar BLOCKER.

---

### Resources

Verificar.

assets.

textures.

lang.

models.

voice.

metadata.

manifest.

---

### Configuración Forge

Verificar.

mods.toml.

pack.mcmeta.

mixins.

META-INF.

access transformers.

---

### Gradle

Verificar.

ForgeGradle.

Mappings.

Repositories.

Dependencies.

Java Version.

Source Sets.

---

### Build Output

Verificar.

jar.

resources.

manifest.

version.

modid.

---

# CAPÍTULO 4 — DEPENDENCY AUDIT

## Objetivo

Eliminar dependencias rotas.

---

## Analizar completamente build.gradle

Buscar.

Dependencias duplicadas.

Versiones incompatibles.

CompileOnly innecesarios.

RuntimeOnly incorrectos.

Implementations huérfanas.

Dependencias transitivas peligrosas.

---

## Clasificación

Cada dependencia recibe.

Requerida.

Opcional.

Desarrollo.

Testing.

Cliente.

Servidor.

---

## Dependencias obligatorias

Forge.

SLF4J.

Jackson.

JUnit.

GameTest.

---

## Dependencias opcionales

CustomNPCs.

Ollama.

Voice Providers.

GeckoLib futura.

---

## Validación automática

Verificar versión mínima y máxima.

Nunca permitir incompatibilidades silenciosas.

---

# CAPÍTULO 5 — CLIENT / SERVER ARCHITECTURE AUDIT

## Este es el problema más importante encontrado.

El análisis mostró que CustomNPCs intenta cargar clases cliente dentro del servidor.

Esto jamás puede volver a ocurrir.

---

## Nueva arquitectura

Separar completamente.

```text
samuraiai
│
├── common
├── client
├── server
├── integration
├── foundation
└── api
```

Nada del cliente puede vivir fuera de client.

---

## Common

Solo contiene.

Brain.

Memory.

Goals.

Behaviors.

Events.

Runtime.

Config.

API.

Nunca GUI.

Nunca Render.

Nunca sonidos.

Nunca teclas.

---

## Client

Solo cliente.

GUI.

Voice.

Overlay.

Renderer.

KeyMappings.

Particles.

Tooltips.

HUD.

Chat.

---

## Server

Solo servidor.

World.

NPC Runtime.

AI Tick.

Persistence.

Server Commands.

Scheduler.

---

## Integration

Adapters.

CustomNPCs.

Future Mods.

Voice Providers.

Ollama Providers.

Nunca lógica principal.

---

## API

Interfaces públicas.

Eventos públicos.

Builders.

Extensiones.

Nunca implementación.

---

# CAPÍTULO 6 — SIDED BOOTSTRAP SYSTEM

Eliminar inicialización mezclada.

Crear dos Bootstrap completamente separados.

---

## Client Bootstrap

Responsabilidades.

Voice.

GUI.

Renderer.

Overlay.

Keybindings.

Client Config.

Debug Overlay.

Chat Button.

Startup Toasts.

---

## Server Bootstrap

Responsabilidades.

NPC Runtime.

Brain.

Scheduler.

Persistence.

Lifecycle.

Commands.

Diagnostics.

---

## Common Bootstrap

Responsabilidades.

Config.

Foundation Audit.

Module Registry.

Services.

Events.

API.

No GUI.

---

## Bootstrap Flow

Forge Event.

↓

Common Bootstrap.

↓

Foundation Audit.

↓

Module Loader.

↓

Detectar lado.

↓

Client Bootstrap.

o

Server Bootstrap.

Nunca ambos.

---

# CAPÍTULO 7 — SIDE GUARD SYSTEM

Crear un sistema automático que impida cargar clases del lado incorrecto.

---

## SideGuard

Crear utilidades.

isClient()

isServer()

requireClient()

requireServer()

assertSide()

---

## Validación automática

Cada módulo registra el lado permitido.

Si intenta cargarse en otro lado.

Generar BLOCKER.

---

## Reflection Guard

Nunca usar reflexión insegura para cargar GUI.

Preparar carga segura.

---

# CAPÍTULO 8 — CUSTOMNPCS COMPATIBILITY LAYER

Reescribir completamente la integración.

---

## Objetivo

SamuraiAI funciona.

Con CustomNPCs.

Sin CustomNPCs.

Con futuras versiones compatibles.

---

## Nueva capa

```text
integration/customnpcs

CustomNPCBridge
CustomNPCAdapter
CustomNPCAvailability
CustomNPCReflection
CustomNPCFactory
CustomNPCController
CustomNPCCompatibilityReport
```

---

## Availability Detector

Detectar.

Mod instalado.

Versión.

Clases disponibles.

API válida.

Errores.

---

## Reflection Layer

Nunca importar directamente clases opcionales desde el núcleo.

Toda comunicación ocurre mediante adaptadores.

---

## Factory

Solo crea adaptadores si el mod existe.

Si no existe.

Usar Dummy Adapter.

---

## Dummy Adapter

No hace nada.

Permite iniciar SamuraiAI normalmente.

---

# CAPÍTULO 9 — MODULE LOADER

Crear un cargador profesional de módulos.

---

## Módulos iniciales

Brain.

Dialogue.

Voice.

CustomNPCs.

Debug.

Ollama.

Tests.

Developer Tools.

---

## Estados

NOT_FOUND

DISABLED

LOADING

READY

FAILED

INCOMPATIBLE

---

## Dependencias entre módulos

Voice depende de Client.

Brain depende de Common.

CustomNPCs depende de módulo opcional.

Ollama depende de configuración.

Todo definido en metadata.

---

# CAPÍTULO 10 — MODULE METADATA SYSTEM

Cada módulo tendrá un descriptor.

Debe incluir.

ID.

Nombre.

Versión.

Autor.

Dependencias.

Lado permitido.

Estado.

Prioridad.

Inicializador.

---

# CAPÍTULO 11 — RESOURCE MANAGER

Crear un administrador global de recursos.

---

## Responsabilidades

Crear carpetas.

Mover recursos.

Extraer recursos internos.

Actualizar recursos.

Verificar hashes.

Eliminar temporales.

Gestionar caché.

---

## Directorios administrados

config.

voice.

cache.

logs.

diagnostics.

reports.

temp.

downloads.

assets.

---

## Integridad

Todo recurso posee SHA256.

Verificar al iniciar.

Reemplazar automáticamente recursos corruptos.

---

# CAPÍTULO 12 — STARTUP DIAGNOSTICS ENGINE

Crear el sistema de diagnóstico profesional del proyecto.

---

## Objetivo

Antes de cargar SamuraiAI generar un diagnóstico completo.

---

## Diagnósticos obligatorios

Sistema operativo.

Java.

Forge.

RAM.

CPU.

GPU.

Versión Minecraft.

Mods.

Voice.

Ollama.

CustomNPCs.

Configuración.

Permisos.

Espacio en disco.

Recursos.

Hash.

Network.

---

## Reporte JSON

Crear automáticamente.

startup_report.json

Debe contener todo el diagnóstico.

---

## Reporte Markdown

startup_report.md

Legible para desarrolladores.

---

## Toast inicial

Mostrar únicamente errores importantes al jugador.

No saturar pantalla.

---

# CAPÍTULO 13 — FOUNDATION REPORT GENERATOR

Al finalizar la auditoría crear un reporte maestro.

---

## FOUNDATION_AUDIT.md

Debe contener.

Resumen.

Tiempo de auditoría.

Versión.

Errores.

Warnings.

Módulos.

Dependencias.

Estado.

Checklist.

Recomendaciones.

---

## FOUNDATION_STATUS.json

Archivo utilizado por futuras fases.

Indica.

READY

BLOCKED

PARTIAL

FAILED

La Fase 2 únicamente podrá comenzar si el estado es READY.

---

# CAPÍTULO 14 — DOCUMENTACIÓN AUTOMÁTICA

Crear carpeta.

docs/phase1_7/

Documentos obligatorios generados por Codex.

FOUNDATION_ARCHITECTURE.md

AUDIT_ENGINE.md

CLIENT_SERVER_ARCHITECTURE.md

CUSTOMNPC_COMPATIBILITY.md

MODULE_LOADER.md

RESOURCE_MANAGER.md

STARTUP_DIAGNOSTICS.md

FOUNDATION_REPORT.md

BOOTSTRAP_SYSTEM.md

DEPENDENCY_AUDIT.md

PROJECT_STRUCTURE_AUDIT.md

CHANGELOG_PHASE1_7.md

Cada documento debe escribirse automáticamente durante la implementación.

---

# CAPÍTULO 15 — CHECKLIST DE IMPLEMENTACIÓN

Codex no puede marcar la Fase 1.7 como terminada hasta completar todas estas verificaciones.

### Arquitectura

* Auditoría creada.
* Registry creado.
* Pipeline creado.
* Bootstrap separado.
* Client separado.
* Server separado.

### CustomNPCs

* Funciona con el mod instalado.
* Funciona sin el mod.
* No carga clases cliente en servidor.

### Recursos

* ResourceManager crea carpetas.
* Hash verificado.
* Recursos corruptos reemplazados.

### Diagnósticos

* JSON generado.
* Markdown generado.
* Logs generados.

### Foundation Report

* FOUNDATION_STATUS.json creado.
* FOUNDATION_AUDIT.md creado.

---

# RESULTADO ESPERADO DE ESTA PRIMERA IMPLEMENTACIÓN DE LA FASE 1.7

Al finalizar esta parte, SamuraiAI ya no inicia directamente Forge y luego sus módulos. Primero ejecuta un sistema completo de auditoría llamado **Foundation Audit Engine**, verifica la arquitectura, dependencias, recursos, compatibilidad cliente/servidor y módulos opcionales, genera reportes técnicos y solo entonces permite que el proyecto continúe con su inicialización. Esta implementación elimina la base del error de CustomNPCs encontrado por Codex y establece la infraestructura que todas las siguientes partes de la Fase 1.7 utilizarán.

---

# =========================================================
# PARTE 2 — VOICE CORE COMPLETION, ASYNC ENGINE Y THREAD SAFETY
# =========================================================

## PARTE 2 — Voice Core Completion, Async Engine y Thread Safety

**Versión:** Foundation v1.7

**Estado:** Continuación oficial del documento Foundation Complete.

**Objetivo:** Cerrar completamente el sistema de voz, eliminar todos los problemas de concurrencia encontrados por Codex y construir la infraestructura asíncrona definitiva de SamuraiAI.

---

# CAPÍTULO 16 — VOICE CORE COMPLETION

## Introducción

El sistema de voz agregado en la Fase 1.6 introdujo la interfaz del micrófono, el bootstrap automático y la arquitectura general del Voice Core. Sin embargo, la auditoría realizada por Codex detectó tres problemas críticos:

1. La carga del modelo puede bloquear el hilo principal del cliente.
2. La actualización automática de modelos quedó incompleta.
3. La pantalla de configuración y el administrador de modelos no están terminados.

Esta sección convierte el sistema de voz en un componente completamente estable, profesional y listo para producción.

---

## Filosofía del Voice Core

El sistema de voz debe cumplir una regla absoluta:

**Todo ocurre automáticamente para el usuario.**

El jugador únicamente instala `SamuraiAI.jar`.

Nunca instalar programas externos.

Nunca ejecutar scripts.

Nunca abrir una consola.

Nunca pedir rutas manuales.

Todo debe ocurrir desde SamuraiAI.

---

# Arquitectura definitiva del Voice Core

Crear una estructura completamente modular.

```text
client/voice
│
├── bootstrap
├── engine
├── installer
├── downloader
├── models
├── audio
├── recognition
├── gui
├── diagnostics
├── cache
├── threading
└── config
```

Cada paquete tiene una responsabilidad única.

---

# VoiceBootstrapService definitivo

Este servicio ya no solo crea carpetas.

Ahora administra el ciclo completo de vida del sistema de voz.

## Responsabilidades

* Primera instalación.
* Verificación de integridad.
* Reparación automática.
* Descarga inicial.
* Migraciones.
* Compatibilidad de versiones.
* Limpieza de caché.
* Inicialización del motor.
* Diagnóstico.
* Estado READY.

Nunca realiza reconocimiento.

---

## Ciclo de vida del Voice Core

Estados completos:

```text
NOT_INSTALLED
BOOTSTRAPPING
CHECKING_FILES
DOWNLOADING_MODEL
VERIFYING_MODEL
EXTRACTING_RESOURCES
INITIALIZING_ENGINE
READY
UPDATING
REPAIRING
FAILED
DISABLED
```

Todo el sistema consulta este estado.

---

# VoiceInstallationService

Crear un servicio especializado únicamente para instalación.

Responsabilidades:

* Crear directorios.
* Crear archivos iniciales.
* Descargar manifiestos.
* Registrar versión instalada.
* Instalar idioma por defecto.
* Verificar espacio disponible.
* Recuperar instalaciones dañadas.

Nunca cargar modelos.

---

# VoiceEngineManager

Administrador absoluto del motor.

Debe conocer únicamente una implementación activa.

## Funciones

Registrar motores.

Seleccionar motor.

Cambiar motor.

Desactivar motor.

Actualizar motor.

Obtener estado.

Obtener idioma.

Obtener versión.

Preparado para múltiples proveedores.

---

# Embedded Speech Engine

El motor de reconocimiento se integra completamente dentro de SamuraiAI.

## Restricciones

No lanzar procesos externos.

No ejecutar ejecutables.

No depender de instalación del usuario.

Toda interacción ocurre mediante clases Java.

---

# VoiceEngineLoader

Separar la carga del motor del Bootstrap.

Responsabilidades:

* Cargar librerías.
* Inicializar recursos.
* Reservar memoria.
* Liberar memoria.
* Recargar modelo.

Nunca bloquear la interfaz.

---

# CAPÍTULO 17 — MODEL MANAGER PROFESIONAL

## Objetivo

Construir un administrador completo de modelos de voz.

---

## VoiceModelManager

Responsabilidades:

Registrar modelos.

Instalar modelos.

Eliminar modelos.

Actualizar modelos.

Verificar modelos.

Cambiar idioma.

Obtener metadatos.

Gestionar versiones.

---

## Modelo por idioma

Cada idioma posee un identificador único.

Ejemplos:

Spanish.

English.

Japanese.

Chinese.

Korean.

Portuguese.

French.

German.

Italian.

Russian.

Preparar más idiomas.

---

## Metadata del modelo

Cada modelo debe almacenar:

Nombre.

Idioma.

Versión.

Hash.

Fecha.

Tamaño.

Compatibilidad.

Estado.

Ruta.

Nunca información duplicada.

---

# Model Manifest

Crear un manifest central.

Debe contener toda la información de modelos disponibles.

Nunca escribir URLs dentro del código.

El manifest controla todo.

---

## Integridad de modelos

Crear VoiceIntegrityChecker.

Debe validar:

SHA256.

Tamaño.

Versión.

Firma.

Corrupción.

Archivos faltantes.

Si falla cualquier verificación.

Eliminar automáticamente el modelo.

---

## Reparación automática

Cuando un modelo está dañado.

El flujo debe ser:

Eliminar.

↓

Descargar nuevamente.

↓

Verificar.

↓

Instalar.

↓

Recargar.

Todo automático.

---

# CAPÍTULO 18 — DOWNLOAD MANAGER

## Objetivo

Descargar recursos de manera profesional.

---

## VoiceDownloadManager

Debe soportar:

Descarga única.

Descarga reanudable.

Cancelación.

Progreso.

Velocidad.

Timeout.

Reintentos.

Integridad.

---

## Reintentos inteligentes

Intentar nuevamente cuando:

Internet perdido.

Timeout.

Archivo corrupto.

Servidor temporalmente inaccesible.

No repetir infinitamente.

---

## Progreso visual

Crear un sistema de progreso.

Información mostrada:

Archivo.

Porcentaje.

Velocidad.

Tiempo restante.

Estado.

No congelar la GUI.

---

## Caché de descargas

Guardar descargas temporales.

Eliminar automáticamente cuando finalicen.

Nunca dejar basura.

---

# CAPÍTULO 19 — VOICE CONFIGURATION CENTER

Crear una configuración completamente nueva.

---

## VoiceConfig

Separar configuración del resto del proyecto.

Opciones:

Idioma.

Micrófono.

Sensibilidad.

Ganancia.

Push To Talk.

Modo automático.

Overlay.

Debug.

Latencia máxima.

Tiempo máximo.

Modelo activo.

Auto Update.

---

## Migración automática

Si cambia la versión.

Actualizar configuración.

Mantener preferencias del usuario.

Nunca perder configuración.

---

## Restauración

Agregar restauración automática de valores por defecto.

---

# CAPÍTULO 20 — AUDIO DEVICE MANAGER

Administrar completamente dispositivos de entrada.

---

## Funciones

Detectar dispositivos.

Cambiar dispositivo.

Reconectar.

Actualizar lista.

Recordar dispositivo favorito.

Notificar cambios.

---

## Hot Swap

Si el micrófono cambia mientras Minecraft está abierto.

Reconectar automáticamente.

No cerrar Voice Core.

---

## Verificación

Mostrar información:

Nombre.

Frecuencia.

Canales.

Formato.

Estado.

---

# CAPÍTULO 21 — AUDIO PIPELINE PROFESIONAL

Separar completamente procesamiento de audio.

---

## Etapas

Captura.

↓

Normalización.

↓

Filtro de ruido.

↓

Voice Activity Detection.

↓

Segmentación.

↓

Reconocimiento.

↓

Texto.

Cada etapa independiente.

---

## AudioNormalizer

Responsabilidad única.

Ajustar volumen.

Eliminar saturación.

Preparar audio para reconocimiento.

---

## NoiseReductionPipeline

Crear infraestructura.

No implementar IA adicional.

Preparar filtros.

---

## Voice Activity Detector

Detectar cuándo comienza y termina la voz.

Eliminar silencios.

Reducir tiempo de procesamiento.

---

## Audio Buffer Manager

Administrar buffers.

Evitar pérdidas.

Evitar crecimiento infinito.

---

# CAPÍTULO 22 — VOICE THREADING ENGINE

## Este capítulo corrige uno de los errores encontrados por Codex.

La carga del modelo nunca debe ejecutarse en Render Thread.

---

## Nueva arquitectura de hilos

```text
Render Thread
     │
Client Thread
     │
Voice Dispatcher
     │
Voice Worker Pool
     │
Recognition Worker
     │
Audio Worker
```

Cada hilo tiene responsabilidades exclusivas.

---

## VoiceThreadDispatcher

Crear un despachador especializado.

Responsabilidades:

Enviar tareas.

Cancelar tareas.

Esperar tareas.

Priorizar tareas.

Reintentos.

---

## Worker Pools

Separar pools.

Audio Pool.

Recognition Pool.

Download Pool.

Cache Pool.

Diagnostics Pool.

Nunca compartir pools.

---

## Future Manager

Administrar todas las tareas asíncronas.

Registrar.

Cancelar.

Esperar.

Timeout.

Limpiar.

---

## Cancellation Tokens

Toda tarea debe poder cancelarse.

Cuando:

Jugador cierra chat.

Jugador sale del mundo.

Minecraft se cierra.

Servidor desconecta.

Modelo cambia.

---

# CAPÍTULO 23 — THREAD SAFETY AUDIT

Auditar completamente todos los hilos del proyecto.

---

## Clases obligatorias a revisar

Voice.

Dialogue.

Brain.

Scheduler.

Memory.

EventBus.

AI Queue.

Lifecycle.

Runtime.

Controllers.

---

## Reglas absolutas

Nunca modificar entidades desde HTTP.

Nunca modificar GUI desde Worker.

Nunca modificar Runtime desde Download Thread.

Siempre volver al hilo correcto.

---

## ThreadGuard

Crear utilidades.

assertClientThread().

assertServerThread().

assertWorkerThread().

assertRenderThread().

Registrar errores automáticamente.

---

## Deadlock Prevention

Crear reglas para evitar bloqueos.

Nunca esperar Future dentro del Render Thread.

Nunca bloquear Client Tick.

Nunca bloquear Server Tick.

---

# CAPÍTULO 24 — GLOBAL ASYNC ENGINE

Este sistema deja de pertenecer al Voice Core.

Ahora pertenece a todo SamuraiAI.

---

## AsyncEngine

Nuevo paquete:

foundation.async

Responsabilidades:

Pools.

Dispatcher.

Scheduler.

Timeouts.

Cancellation.

Metrics.

Retry Policy.

Future Registry.

---

## AsyncDispatcher

Entrada única para tareas asíncronas.

Todos los sistemas deben usarlo.

Nunca crear Executors manualmente.

---

## AsyncTask

Crear una representación estándar de tareas.

Debe incluir:

ID.

Módulo.

Prioridad.

Estado.

Tiempo.

Timeout.

Cancelación.

Resultado.

---

## Async Metrics

Registrar:

Tiempo promedio.

Tiempo máximo.

Errores.

Cancelaciones.

Reintentos.

Hilos activos.

Uso de CPU.

---

# CAPÍTULO 25 — AI REQUEST QUEUE PROFESIONAL

La cola actual queda reemplazada por una implementación profesional.

---

## AIQueueManager

Responsabilidades:

FIFO.

Prioridades.

Cancelación.

Timeout.

Persistencia temporal.

Métricas.

Debug.

---

## Estados de solicitudes

PENDING.

QUEUED.

RUNNING.

WAITING_RESPONSE.

COMPLETED.

FAILED.

TIMEOUT.

CANCELLED.

---

## Cola por NPC

Cada NPC mantiene su propia cola conversacional.

Nunca mezclar conversaciones.

---

## Cola global

Existe una cola superior.

Distribuye solicitudes entre NPCs.

Preparada para cientos de NPC.

---

## Timeout Manager

Cancelar solicitudes lentas.

Notificar Brain.

Crear evento AIRequestTimeout.

---

## Retry Policy

Reintentos inteligentes únicamente cuando corresponda.

No repetir respuestas ya procesadas.

---

# CAPÍTULO 26 — TASK SCHEDULER PROFESIONAL

Crear Scheduler unificado.

---

## SchedulerRegistry

Registrar tareas.

Brain.

Voice.

Dialogue.

Diagnostics.

Cleanup.

Persistence futura.

---

## Prioridades

CRITICAL.

HIGH.

NORMAL.

LOW.

BACKGROUND.

Cada módulo usa prioridades diferentes.

---

## Tick Budget

Asignar presupuesto por tick.

Nunca saturar Minecraft.

Preparado para muchos NPC.

---

# CAPÍTULO 27 — FOUNDATION THREAD REPORT

Crear reporte especializado.

---

## THREAD_AUDIT.md

Debe listar.

Todos los hilos.

Pools.

Tareas activas.

Tiempo promedio.

Errores.

Deadlocks.

Cancelaciones.

---

## THREAD_STATUS.json

Estado utilizado por futuras auditorías.

READY.

WARNING.

FAILED.

---

# CAPÍTULO 28 — DOCUMENTACIÓN OBLIGATORIA

Crear automáticamente.

docs/phase1_7/threading/

VOICE_CORE_COMPLETION.md

VOICE_MODEL_MANAGER.md

VOICE_DOWNLOAD_MANAGER.md

VOICE_AUDIO_PIPELINE.md

VOICE_THREADING.md

ASYNC_ENGINE.md

THREAD_SAFETY.md

AI_QUEUE_COMPLETION.md

TASK_SCHEDULER.md

THREAD_AUDIT.md

CHANGELOG_THREADING.md

Todos los documentos deben explicar arquitectura, flujo, clases modificadas y pruebas.

---

# CAPÍTULO 29 — CHECKLIST DE IMPLEMENTACIÓN

Codex debe verificar obligatoriamente:

### Voice Core

* Bootstrap completo.
* Instalación automática.
* Model Manager completo.
* Download Manager completo.
* Integridad automática.
* Reparación automática.
* Configuración completa.
* Pantalla de configuración completa.

### Threading

* VoiceThreadDispatcher creado.
* AsyncEngine creado.
* Worker Pools creados.
* Cancellation Tokens implementados.
* ThreadGuard implementado.
* Thread Audit completado.

### AI Queue

* FIFO implementada.
* Cola por NPC.
* Cola global.
* Timeout Manager.
* Retry Policy.
* Métricas.

### Scheduler

* SchedulerRegistry creado.
* Tick Budget implementado.
* Prioridades implementadas.

---

# RESULTADO ESPERADO DE ESTA SEGUNDA IMPLEMENTACIÓN DE LA FASE 1.7

Al finalizar esta parte de Foundation Complete, SamuraiAI tendrá un sistema de voz completamente terminado y estable, un motor de concurrencia unificado para todo el proyecto, una AI Queue profesional preparada para cientos de NPCs y una infraestructura de hilos que elimina los problemas detectados por Codex. Ningún modelo bloqueará el cliente, ninguna tarea asíncrona modificará Minecraft desde el hilo incorrecto y toda la arquitectura quedará lista para la tercera parte de la Fase 1.7.

---

# =========================================================
# PARTE 3 — NPC LIFECYCLE, EVENTBUS 2.0, RUNTIME ENGINE Y DEVELOPER TOOLKIT
# =========================================================

## PARTE 3 — NPC Lifecycle Completion, EventBus Completion, Runtime Engine y Developer Toolkit

**Versión:** Foundation v1.7

**Estado:** Continuación oficial del documento Foundation Complete.

**Objetivo:** Construir el ciclo de vida definitivo de todos los NPC, eliminar fugas de memoria y eventos huérfanos, crear el Runtime Manager definitivo y desarrollar el sistema de herramientas internas que utilizarán Codex y los desarrolladores de SamuraiAI.

---

# CAPÍTULO 30 — EL LIFECYCLE DEFINITIVO DE LOS NPC

## Filosofía

Hasta este momento los NPC pueden existir incluso cuando su Runtime ya no debería existir. El análisis detectó el problema conocido como **NPC Fantasma**.

Un NPC fantasma es una entidad cuyo Runtime continúa activo después de que el NPC físico desapareció del mundo o fue eliminado.

Este comportamiento nunca debe volver a ocurrir.

El Lifecycle se convierte en el sistema más importante del proyecto después del Brain.

---

## Arquitectura del Lifecycle Engine

Crear un paquete completamente nuevo.

```text
yadi.samuraiai.npc.lifecycle

lifecycle/
├── NPCLifecycleManager.java
├── NPCLifecycleService.java
├── NPCLifecycleState.java
├── NPCLifecycleTransition.java
├── NPCLifecycleValidator.java
├── NPCLifecycleEventPublisher.java
├── NPCLifecycleDiagnostics.java
├── NPCRemovalPipeline.java
├── NPCActivationPipeline.java
├── NPCUnloadPipeline.java
├── NPCReloadPipeline.java
└── LifecycleMetrics.java
```

Toda la vida de un NPC debe pasar únicamente por este sistema.

Nunca eliminar entidades directamente desde otros módulos.

---

## Estados oficiales del Lifecycle

Cada NPC tendrá exactamente un estado.

Estados permitidos:

UNINITIALIZED

CREATING

INITIALIZING

LOADING_RUNTIME

ACTIVE

IDLE

PAUSED

UNLOADING

REMOVING

REMOVED

ERROR

INVALID

Ningún NPC puede existir sin estado.

---

## Reglas absolutas del Lifecycle

* No puede haber dos Runtime para un mismo NPC.
* No puede haber dos UUID registrados.
* No puede existir Runtime sin entidad física.
* No puede existir Controller sin Runtime.
* No puede existir Brain sin Lifecycle ACTIVE.
* Todo cambio publica un evento.
* Toda transición queda registrada en logs.

---

## Pipeline de creación del NPC

Crear un pipeline oficial.

```text
Spawn Request
      ↓
Lifecycle Manager
      ↓
Entity Creation
      ↓
Runtime Creation
      ↓
Brain Creation
      ↓
Controller Creation
      ↓
Registration
      ↓
Lifecycle ACTIVE
```

Cada paso puede fallar.

Si uno falla se ejecuta rollback completo.

---

## Rollback automático

Si la creación falla en cualquier etapa.

Eliminar Runtime.

Eliminar Controller.

Eliminar registros.

Cancelar AI Queue.

Cancelar Voice Tasks.

Publicar NPCInitializationFailedEvent.

---

# CAPÍTULO 31 — RUNTIME MANAGER COMPLETO

## Objetivo

Crear el administrador absoluto de Runtime.

---

## RuntimeManager

Responsabilidades.

Crear Runtime.

Destruir Runtime.

Actualizar Runtime.

Pausar Runtime.

Reactivar Runtime.

Migrar Runtime.

Recuperar Runtime.

Nunca permitir referencias huérfanas.

---

## Runtime Registry

Mantener índices.

UUID.

Entity ID.

Nombre.

Tipo.

Mundo.

Estado.

Chunk.

Todo sincronizado.

---

## Runtime Validator

Verificar constantemente.

Entidad válida.

Brain válido.

Controller válido.

Memory válida.

Queue válida.

Lifecycle válido.

Si encuentra inconsistencias.

Entrar en modo reparación.

---

## Runtime Repair Engine

Reparar automáticamente.

Recrear Controller.

Cancelar Queue.

Eliminar Runtime roto.

Recargar Runtime.

Registrar diagnóstico.

---

# CAPÍTULO 32 — NPC REMOVAL PIPELINE

Eliminar NPC ya no significa llamar remove().

Crear un pipeline completo.

---

## Flujo oficial

```text
Remove Request
      ↓
Lifecycle Transition
      ↓
Cancel Tasks
      ↓
Cancel Queue
      ↓
Cancel Futures
      ↓
Publish Events
      ↓
Save Temporary State
      ↓
Remove Entity
      ↓
Destroy Runtime
      ↓
Clean Registries
      ↓
Lifecycle REMOVED
```

Todo obligatorio.

---

## Cancelación completa

Cancelar.

AI Requests.

Voice Sessions.

Behavior Tasks.

Scheduler Jobs.

Pathfinding.

Dialogue Sessions.

Delayed Events.

No dejar tareas pendientes.

---

## Garbage Collection Interna

Crear un sistema propio.

Eliminar referencias.

Weak References.

Caches.

Temporary Memory.

Metrics.

Nunca depender únicamente del GC de Java.

---

# CAPÍTULO 33 — NPC ACTIVATION / DEACTIVATION ENGINE

## Objetivo

Permitir activar y desactivar NPC sin destruirlos.

---

## Estados nuevos

ACTIVE

SLEEPING

HIBERNATING

INACTIVE

Dormido significa.

No procesa Brain.

No procesa Behaviors.

Mantiene memoria.

Preparado para chunks lejanos.

---

## Chunk Activation System

Cuando un Chunk carga.

Activar NPC.

Cuando descarga.

Dormir NPC.

No destruir Runtime si no es necesario.

Preparado para cientos de NPC.

---

## Runtime Hibernation

Guardar únicamente estado temporal.

Liberar recursos pesados.

Mantener identidad.

---

# CAPÍTULO 34 — MEMORY CLEANUP ENGINE

## Objetivo

Eliminar pérdidas de memoria.

---

## Cleanup Manager

Responsabilidades.

Limpiar referencias.

Limpiar diálogos.

Limpiar queues.

Limpiar eventos.

Limpiar overlays.

Limpiar cachés.

---

## Tipos de memoria

Conversation Cache.

Perception Cache.

Runtime Cache.

Voice Cache.

AI Cache.

Behavior Cache.

Event Cache.

Cada una tiene limpieza independiente.

---

## Cleanup Scheduler

Ejecutar periódicamente.

Nunca durante Render Thread.

Nunca durante Brain Tick.

---

# CAPÍTULO 35 — EVENTBUS 2.0

El EventBus se convierte en la columna vertebral del proyecto.

---

## Arquitectura

```text
foundation/events

EventBus

EventRegistry

EventDispatcher

EventListenerRegistry

EventPriority

EventContext

EventMetrics

EventDiagnostics
```

Todos los módulos usan este EventBus.

---

## Principios

Eventos inmutables.

Eventos asincrónicos cuando corresponda.

Eventos sincronizados cuando afecten Minecraft.

Eventos tipados.

Nunca usar Strings.

---

## Prioridades

MONITOR.

LOWEST.

LOW.

NORMAL.

HIGH.

HIGHEST.

CRITICAL.

Preparado para interceptores.

---

## Event Dispatcher

Responsabilidades.

Publicar.

Cancelar.

Esperar.

Priorizar.

Medir tiempo.

Reintentar.

---

## Event Registry

Registrar automáticamente eventos mediante escaneo de clases.

No registrar manualmente uno por uno.

---

# CAPÍTULO 36 — CATÁLOGO COMPLETO DE EVENTOS

Crear todos los eventos oficiales del proyecto.

---

## Lifecycle Events

NPCSpawnRequestedEvent.

NPCCreatedEvent.

NPCInitializedEvent.

NPCActivatedEvent.

NPCPausedEvent.

NPCHibernatedEvent.

NPCUnloadedEvent.

NPCRemovedEvent.

NPCDestroyedEvent.

NPCReloadedEvent.

---

## Brain Events

BrainStartedEvent.

BrainStoppedEvent.

GoalSelectedEvent.

GoalChangedEvent.

DecisionCompletedEvent.

DecisionFailedEvent.

BrainTickEvent.

---

## Behavior Events

BehaviorStartedEvent.

BehaviorPausedEvent.

BehaviorInterruptedEvent.

BehaviorCompletedEvent.

BehaviorCancelledEvent.

TaskStartedEvent.

TaskCompletedEvent.

ActionExecutedEvent.

ActionFailedEvent.

---

## Dialogue Events

DialogueStartedEvent.

DialogueEndedEvent.

DialogueInterruptedEvent.

DialogueTimeoutEvent.

PlayerMessageReceivedEvent.

NPCResponseGeneratedEvent.

---

## Voice Events

VoiceReadyEvent.

VoiceStartedEvent.

VoiceStoppedEvent.

VoiceRecognizedEvent.

VoiceErrorEvent.

VoiceModelInstalledEvent.

VoiceModelUpdatedEvent.

---

## Queue Events

AIRequestQueuedEvent.

AIRequestStartedEvent.

AIRequestCompletedEvent.

AIRequestCancelledEvent.

AIRequestTimeoutEvent.

AIRequestFailedEvent.

---

## Runtime Events

RuntimeCreatedEvent.

RuntimeDestroyedEvent.

RuntimeRecoveredEvent.

RuntimeCorruptedEvent.

RuntimeRepairEvent.

---

## Scheduler Events

TaskScheduledEvent.

TaskExecutedEvent.

TaskCancelledEvent.

TickBudgetExceededEvent.

SchedulerPausedEvent.

SchedulerResumedEvent.

---

## Debug Events

DebugSessionStartedEvent.

DebugSessionEndedEvent.

ProfilerStartedEvent.

ProfilerStoppedEvent.

DiagnosticGeneratedEvent.

---

# CAPÍTULO 37 — EVENT DIAGNOSTICS

Crear un sistema que observe el EventBus.

---

## Event Diagnostics

Mostrar.

Eventos publicados.

Eventos cancelados.

Tiempo promedio.

Listeners activos.

Errores.

Loops.

Eventos lentos.

---

## Event Loop Detector

Detectar ciclos infinitos.

Evento publica evento.

Evento vuelve a publicar.

Romper automáticamente.

---

## Event Profiler

Medir.

Tiempo por evento.

Listener más lento.

Cantidad de publicaciones.

Frecuencia.

---

# CAPÍTULO 38 — RUNTIME DIAGNOSTICS CENTER

Crear un centro de diagnóstico interno.

---

## Runtime Diagnostics

Mostrar.

NPC activos.

Runtime activos.

Dormidos.

Pausados.

Corruptos.

Memoria.

CPU.

Colas.

Eventos.

---

## Diagnostic Snapshot

Generar un snapshot completo.

Guardar JSON.

Guardar Markdown.

Comparar snapshots.

---

## Runtime Health Score

Cada Runtime obtiene un estado.

HEALTHY.

WARNING.

DEGRADED.

FAILED.

Nunca usar puntuaciones para IA.

Solo diagnóstico.

---

# CAPÍTULO 39 — PERFORMANCE BUDGET SYSTEM

## Objetivo

Evitar que SamuraiAI destruya el rendimiento.

---

## Budget Manager

Asignar presupuesto.

Brain.

Voice.

Dialogue.

Behaviors futuros.

Scheduler.

Perception.

Events.

---

## Budget Categories

CPU.

RAM.

Tick Time.

Queue.

Audio.

Pathfinding futuro.

---

## Tick Budget Analyzer

Medir cuánto consume cada módulo.

Registrar.

Advertir.

Cancelar tareas secundarias si excede presupuesto.

---

## Slow Task Detector

Detectar tareas lentas.

Generar reporte.

---

# CAPÍTULO 40 — LOGGER 2.0

Reescribir completamente el sistema de logs.

---

## Categorías oficiales

FOUNDATION.

BOOTSTRAP.

VOICE.

AI.

BRAIN.

LIFECYCLE.

RUNTIME.

EVENTBUS.

QUEUE.

MEMORY.

CONFIG.

CUSTOMNPCS.

DEBUG.

PERFORMANCE.

SCHEDULER.

Cada categoría configurable.

---

## Formato de logs

Timestamp.

Thread.

NPC UUID.

Módulo.

Nivel.

Mensaje.

Duración.

---

## Log Rotation

Crear rotación automática.

No crecer indefinidamente.

---

# CAPÍTULO 41 — DEBUG CONSOLE

Crear una consola de desarrollo dentro de Minecraft.

---

## Objetivo

Permitir inspeccionar SamuraiAI sin salir del juego.

---

## Información disponible

NPCs.

Brain.

Goals.

Runtime.

Voice.

Queue.

Events.

Performance.

Memory.

Lifecycle.

Configuración.

---

## Comandos Debug

Ver Runtime.

Forzar evento.

Reiniciar Voice.

Mostrar Queue.

Mostrar Threads.

Mostrar Lifecycle.

Limpiar Cache.

Generar Snapshot.

---

# CAPÍTULO 42 — DEVELOPER TOOLKIT

Crear un conjunto de herramientas para desarrollo.

---

## Overlay de Desarrollo

Mostrar información en pantalla.

FPS.

Tick Time.

NPC Count.

Brain Time.

Queue.

Voice Latency.

Event Rate.

Memory Usage.

---

## Inspector de NPC

Seleccionar un NPC.

Ver toda su información.

UUID.

Estado.

Runtime.

Memory.

Relationship.

Emotion.

Goals.

Queue.

Eventos recientes.

---

## Runtime Inspector

Inspeccionar Runtime completo.

---

## Voice Inspector

Ver estado del motor.

Modelo.

Idioma.

Micrófono.

Latencia.

---

# CAPÍTULO 43 — CONFIG VALIDATION ENGINE

Validar toda configuración del proyecto.

---

## ConfigValidator

Verificar.

Valores mínimos.

Máximos.

Tipos.

Dependencias.

Versiones.

Migraciones.

---

## Config Migration

Actualizar automáticamente configuraciones antiguas.

---

# CAPÍTULO 44 — RESOURCE VALIDATION ENGINE

Validar todos los recursos del proyecto.

---

## Recursos auditados

Textures.

Models.

Voice.

Lang.

Metadata.

Icons.

Configs.

---

## SHA Validation

Verificar integridad de todos los recursos.

---

# CAPÍTULO 45 — FOUNDATION SNAPSHOT SYSTEM

Crear snapshots completos del estado del proyecto.

---

## Snapshot contiene

Configuración.

Runtime.

Eventos.

Voice.

Queue.

Lifecycle.

Performance.

Versiones.

Hashes.

---

## Comparador de Snapshots

Permitir comparar dos estados del proyecto.

---

# CAPÍTULO 46 — DOCUMENTACIÓN AUTOMÁTICA

Crear carpeta.

docs/phase1_7/runtime/

Archivos obligatorios.

NPC_LIFECYCLE.md

RUNTIME_MANAGER.md

EVENTBUS_2.md

EVENT_CATALOG.md

EVENT_DIAGNOSTICS.md

PERFORMANCE_BUDGET.md

LOGGER_2.md

DEBUG_CONSOLE.md

DEVELOPER_TOOLKIT.md

CONFIG_VALIDATION.md

RESOURCE_VALIDATION.md

FOUNDATION_SNAPSHOTS.md

CHANGELOG_RUNTIME.md

Cada documento debe describir arquitectura, flujo, clases, eventos y pruebas.

---

# CAPÍTULO 47 — CHECKLIST DE IMPLEMENTACIÓN

### Lifecycle

* Runtime único.
* Eliminación segura.
* Rollback.
* Activación.
* Hibernación.
* Cleanup.

### EventBus

* Registry.
* Dispatcher.
* Prioridades.
* Catálogo completo.
* Diagnósticos.

### Runtime

* Validator.
* Repair Engine.
* Diagnostics.
* Health Score.

### Debug

* Console.
* Overlay.
* Inspector.
* Snapshots.

### Performance

* Tick Budget.
* Slow Task Detector.
* Logs.

---

# RESULTADO ESPERADO DE ESTA TERCERA IMPLEMENTACIÓN DE LA FASE 1.7

Al finalizar esta parte, SamuraiAI tendrá un ciclo de vida completamente seguro para todos los NPC, un EventBus profesional que conecta todos los sistemas internos, un Runtime Manager capaz de detectar y reparar inconsistencias automáticamente, un sistema de diagnóstico y métricas de rendimiento integrado dentro del juego y un Developer Toolkit que permitirá depurar cualquier parte del proyecto sin salir de Minecraft. Esta infraestructura elimina definitivamente los NPC fantasma, los eventos huérfanos y las fugas de memoria detectadas durante la auditoría técnica, dejando el núcleo listo para la certificación final de Foundation Complete.

---

# =========================================================
# PARTE 4A — TESTING FRAMEWORK 2.0, COMPATIBILITY SUITE Y QA AUTOMATION FRAMEWORK
# =========================================================

## Testing Framework 2.0 • Compatibility Suite • QA Automation Framework

**Proyecto:** SamuraiAI

**Minecraft:** Forge 1.19.2

**Java:** Java 17

**Estado:** Continuación oficial de Foundation Complete.

**Prioridad:** CRÍTICA.

**Dependencia:** Todas las Partes anteriores de Foundation Complete deben estar implementadas antes de ejecutar este documento.

---

# PRÓLOGO — LA CERTIFICACIÓN DEL NÚCLEO

Hasta este punto SamuraiAI posee un núcleo arquitectónico completo:

* Foundation Audit Engine.
* Client / Server Architecture.
* Voice Core Embedded.
* Async Engine.
* AI Queue.
* Runtime Manager.
* Lifecycle Engine.
* EventBus 2.0.
* Developer Toolkit.
* Diagnostics Engine.

Sin embargo, una arquitectura no es suficiente.

Necesitamos demostrar que todo funciona correctamente.

Esta parte crea el sistema que certificará automáticamente la estabilidad del proyecto.

A partir de ahora SamuraiAI tendrá un laboratorio interno capaz de probar cada módulo automáticamente.

No es únicamente un conjunto de pruebas.

Es un **Framework de Certificación**.

Cada módulo deberá demostrar que funciona antes de ser considerado estable.

---

# CAPÍTULO 48 — TESTING FRAMEWORK 2.0

## Filosofía del Framework

El objetivo no es comprobar si el código compila.

El objetivo es comprobar que SamuraiAI funciona correctamente en condiciones reales.

Toda funcionalidad nueva deberá tener pruebas.

Toda regresión deberá detectarse automáticamente.

Toda prueba deberá poder ejecutarse desde Codex y desde Gradle.

---

## Objetivos generales

Construir un framework capaz de:

* descubrir pruebas automáticamente.
* ejecutar pruebas por módulos.
* ejecutar pruebas por categoría.
* ejecutar pruebas completas del proyecto.
* ejecutar pruebas de integración.
* ejecutar pruebas de estrés.
* ejecutar pruebas de compatibilidad.
* generar reportes automáticos.
* bloquear compilaciones cuando existan errores críticos.

---

## Arquitectura general

Crear un paquete completamente nuevo.

```text
yadi.samuraiai.foundation.testing

testing/
├── framework/
├── registry/
├── junit/
├── gametest/
├── integration/
├── regression/
├── stress/
├── compatibility/
├── reports/
├── metrics/
├── qa/
└── certification/
```

Todo el sistema de pruebas vive aquí.

Nunca mezclar pruebas con código de producción.

---

# CAPÍTULO 49 — TEST FRAMEWORK CORE

Crear el núcleo del sistema.

## TestingFramework

Será el orquestador absoluto.

Responsabilidades:

* registrar suites.
* descubrir pruebas.
* ejecutar suites.
* ejecutar categorías.
* cancelar pruebas.
* recopilar resultados.
* generar métricas.
* exportar reportes.

Nunca ejecuta lógica de SamuraiAI directamente.

---

## TestRegistry

Registro automático de pruebas.

Debe descubrir mediante anotaciones o registro central todas las pruebas disponibles.

Cada prueba posee:

* ID.
* Nombre.
* Categoría.
* Prioridad.
* Tiempo esperado.
* Dependencias.
* Tags.
* Estado.

---

## Test Categories

Categorías oficiales.

FOUNDATION

VOICE

BRAIN

QUEUE

RUNTIME

LIFECYCLE

EVENTBUS

CONFIG

RESOURCES

PERFORMANCE

COMPATIBILITY

NETWORK

DEBUG

STRESS

REGRESSION

DOCUMENTATION

Cada prueba pertenece al menos a una categoría.

---

## Test Priorities

CRITICAL

HIGH

NORMAL

LOW

BACKGROUND

Las críticas siempre se ejecutan primero.

---

# CAPÍTULO 50 — DISCOVERY ENGINE

Crear un motor que encuentre pruebas automáticamente.

## Objetivos

No registrar pruebas manualmente.

Escanear paquetes.

Registrar clases.

Registrar suites.

Validar duplicados.

---

## Validaciones

Cada prueba debe tener:

ID único.

Categoría.

Descripción.

Timeout.

Resultado esperado.

Si falta alguno.

Generar BLOCKER.

---

# CAPÍTULO 51 — TEST EXECUTION ENGINE

Crear el motor de ejecución.

## Estados de pruebas

PENDING

RUNNING

PASSED

FAILED

SKIPPED

TIMEOUT

CANCELLED

BLOCKED

Todo queda registrado.

---

## Ejecución paralela

Las pruebas compatibles pueden ejecutarse en paralelo.

Las críticas nunca.

Preparar pools separados.

---

## Timeout Manager

Cada prueba tiene tiempo máximo.

Si excede.

Cancelar.

Registrar.

Continuar con la siguiente.

---

# CAPÍTULO 52 — JUNIT CORE SUITE

Crear la suite principal de pruebas unitarias.

## Objetivo

Probar clases aisladas.

Nunca depender de Minecraft.

---

## Suites obligatorias

BrainSuite.

MemorySuite.

EmotionSuite.

RelationshipSuite.

RuntimeSuite.

LifecycleSuite.

EventBusSuite.

QueueSuite.

VoiceSuite.

ConfigSuite.

ResourceSuite.

SchedulerSuite.

---

## Brain Suite

Pruebas obligatorias.

Inicialización.

Cambio de Goal.

Cambio de Estado.

Eventos publicados.

Errores.

Cancelaciones.

---

## Queue Suite

FIFO.

Prioridades.

Timeout.

Cancelación.

Orden.

Persistencia temporal.

---

## EventBus Suite

Registro.

Prioridades.

Cancelación.

Orden.

Loops.

Errores.

---

# CAPÍTULO 53 — FORGE GAMETEST SUITE

Crear GameTests profesionales.

## Filosofía

Probar SamuraiAI dentro de Minecraft.

No simulaciones.

Entidades reales.

Mundos reales.

Eventos reales.

---

## GameTest Registry

Registrar automáticamente.

---

## Categorías

NPC Spawn.

NPC Remove.

Dialogue.

Voice GUI.

Runtime.

Scheduler.

Events.

Pathfinding futuro.

Chunks.

Persistence futura.

---

## Mundo de pruebas

Crear un mundo exclusivo.

No usar mundos del usuario.

Preparar estructuras.

NPCs.

Jugadores simulados.

Bloques.

Entorno.

---

## Spawn Tests

Verificar.

Creación.

UUID.

Runtime.

Brain.

Controller.

Eventos.

Lifecycle.

---

## Removal Tests

Verificar limpieza completa.

Runtime.

Queue.

Eventos.

Cache.

Lifecycle.

---

# CAPÍTULO 54 — INTEGRATION TEST FRAMEWORK

Las pruebas de integración conectan múltiples módulos.

## Suites

Brain + Queue.

Brain + EventBus.

Runtime + Lifecycle.

Voice + Chat.

CustomNPC + Runtime.

Scheduler + Queue.

Diagnostics + Runtime.

---

## Objetivo

Detectar problemas entre módulos.

---

## Integración Brain + Queue

Verificar.

Solicitudes.

Orden.

Cancelación.

Timeout.

Respuesta.

---

## Voice + Chat

Verificar.

Micrófono.

Reconocimiento.

Inserción.

Eventos.

Cancelación.

---

# CAPÍTULO 55 — REGRESSION TEST ENGINE

Crear el motor de regresiones.

## Filosofía

Cada bug corregido genera una prueba permanente.

Nunca permitir que vuelva.

---

## Bug Registry

Registrar.

ID.

Descripción.

Fecha.

Versión.

Clase afectada.

Prueba asociada.

---

## Casos obligatorios

CustomNPC Client Crash.

Voice Blocking Render Thread.

Queue Deadlock.

Lifecycle Ghost NPC.

Event Loop.

Config Migration.

Cada uno posee una prueba.

---

# CAPÍTULO 56 — STRESS TEST FRAMEWORK

Crear pruebas de estrés.

## Objetivo

Romper SamuraiAI antes de que llegue al usuario.

---

## Escenarios

100 NPC.

250 NPC.

500 NPC.

1000 solicitudes.

Voice simultánea.

Eventos masivos.

Scheduler saturado.

Chunks múltiples.

---

## Métricas

CPU.

RAM.

TPS.

Tiempo Brain.

Tiempo Queue.

Tiempo Voice.

Eventos.

---

## Resultado esperado

El framework registra cuándo comienza la degradación.

---

# CAPÍTULO 57 — COMPATIBILITY SUITE

Uno de los sistemas más importantes.

## Objetivo

Garantizar compatibilidad completa.

---

## Compatibilidad Cliente

Verificar.

Minecraft.

Forge.

Voice.

GUI.

Keybindings.

Overlays.

Render.

---

## Compatibilidad Servidor

Servidor dedicado.

Sin GUI.

Sin Voice.

Sin clases cliente.

Sin crashes.

---

## Compatibilidad Singleplayer

Cliente + servidor integrado.

Todos los módulos.

---

## Compatibilidad LAN

Servidor integrado.

Varios jugadores.

Eventos.

Runtime.

---

# CAPÍTULO 58 — CUSTOMNPCS COMPATIBILITY MATRIX

Crear matriz completa.

## Escenarios

CustomNPC instalado.

CustomNPC ausente.

Versión compatible.

Versión incompatible.

API incompleta.

Error Reflection.

---

## Validaciones

Spawn.

Dialogue.

Runtime.

Lifecycle.

Controller.

Removal.

Events.

---

## Resultado

Reporte completo.

---

# CAPÍTULO 59 — VOICE COMPATIBILITY MATRIX

Probar todos los escenarios de Voice Core.

## Casos

Micrófono disponible.

Micrófono ausente.

Cambio de dispositivo.

Idioma español.

Idioma inglés.

Cambio dinámico.

Modelo corrupto.

Modelo inexistente.

Sin Internet.

Con Internet.

---

## GUI Tests

Micrófono.

Overlay.

Push To Talk.

Configuración.

Toast.

---

# CAPÍTULO 60 — OLLAMA COMPATIBILITY SUITE

Preparar pruebas del proveedor IA.

## Casos

Ollama disponible.

Ollama apagado.

Modelo inexistente.

Timeout.

Respuesta lenta.

Respuesta vacía.

Respuesta inválida.

---

## Queue Tests

Reintentos.

Cancelación.

Timeout.

Errores.

---

# CAPÍTULO 61 — CONFIG VALIDATION TESTS

Todas las configuraciones deben probarse.

## Casos

Archivo inexistente.

Migración.

Valores inválidos.

Versiones antiguas.

Valores por defecto.

Restauración.

---

# CAPÍTULO 62 — RESOURCE VALIDATION TESTS

Verificar recursos.

## Recursos

Textures.

Icons.

Lang.

Voice.

Metadata.

Manifest.

Hashes.

---

## SHA Tests

Cada recurso debe pasar SHA.

Si falla.

Reparación automática.

---

# CAPÍTULO 63 — QA AUTOMATION FRAMEWORK

Crear un sistema automático de calidad.

## QAEngine

Responsabilidades.

Ejecutar auditoría.

Ejecutar pruebas.

Comparar resultados.

Generar certificación.

---

## QA Rules

Cada módulo posee reglas.

Foundation.

Voice.

Runtime.

Queue.

EventBus.

Lifecycle.

Scheduler.

---

## QA Metrics

Cantidad de pruebas.

Errores.

Warnings.

Skipped.

Tiempo.

Cobertura.

---

# CAPÍTULO 64 — TEST REPORT GENERATOR

Generar automáticamente documentación.

## Archivos obligatorios

TEST_REPORT.md

JUNIT_REPORT.md

GAME_TEST_REPORT.md

REGRESSION_REPORT.md

STRESS_REPORT.md

COMPATIBILITY_REPORT.md

VOICE_REPORT.md

CUSTOMNPC_REPORT.md

OLLAMA_REPORT.md

QA_REPORT.md

---

## Formato

Resumen.

Tiempo.

Errores.

Warnings.

Resultados.

Cobertura.

Recomendaciones.

---

# CAPÍTULO 65 — QA VALIDATION CHECKLIST

Crear checklist automática.

## Foundation

* Auditoría pasa.
* Bootstrap correcto.
* Módulos registrados.

## Voice

* Voice READY.
* Modelo válido.
* Micrófono válido.

## Runtime

* Sin NPC fantasma.
* Lifecycle correcto.

## EventBus

* Sin loops.
* Eventos registrados.

## Queue

* FIFO correcta.
* Timeout correcto.

## Compatibilidad

* Cliente.
* Servidor.
* CustomNPC.
* Ollama.

Todo obligatorio.

---

# CAPÍTULO 66 — RESULTADO ESPERADO DE LA PARTE 4A

Al finalizar esta implementación SamuraiAI tendrá un laboratorio interno completamente profesional.

El proyecto será capaz de ejecutar cientos de pruebas unitarias, GameTests dentro de Minecraft, pruebas de integración entre módulos, regresiones permanentes, pruebas de estrés y matrices completas de compatibilidad con Forge, CustomNPCs, Voice Core y Ollama.

Cada ejecución generará reportes automáticos en Markdown y JSON que servirán como evidencia técnica para Foundation Complete.

La Parte 4B utilizará estos resultados para construir el sistema definitivo de certificación del proyecto.

---

# =========================================================
# PARTE 4B — PERFORMANCE SUITE 2.0, BENCHMARK ENGINE, RELEASE PIPELINE Y FOUNDATION CERTIFICATE
# =========================================================

## Performance Suite 2.0 • Benchmark Engine • Release Pipeline • Foundation Certificate

**Proyecto:** SamuraiAI

**Minecraft:** Forge 1.19.2

**Java:** Java 17

**Estado:** Cierre oficial de Foundation Complete.

**Prioridad:** CRÍTICA.

**Dependencia:** Las Partes 1, 2, 3 y 4A deben estar implementadas y aprobadas antes de ejecutar esta certificación.

---

# PRÓLOGO — LA CERTIFICACIÓN DEFINITIVA DEL NÚCLEO

Esta es la fase que determina si SamuraiAI está realmente listo para evolucionar.

Hasta este punto ya existen auditorías, Runtime, Voice Core, EventBus, Lifecycle y Testing Framework.

Ahora debemos responder una única pregunta:

> **¿La fundación del proyecto es suficientemente estable para construir la IA física (Behavior Engine)?**

La respuesta ya no depende de una persona.

Depende del sistema de certificación automática.

Foundation Complete crea un proceso similar al de una integración continua profesional: el proyecto ejecuta auditorías, benchmarks, pruebas de rendimiento, pruebas de compatibilidad y genera un certificado oficial.

Si cualquier prueba crítica falla, Foundation Complete queda bloqueado.

---

# CAPÍTULO 73 — PERFORMANCE SUITE 2.0

## Objetivo

Crear un laboratorio interno capaz de medir el rendimiento de SamuraiAI en tiempo real.

El objetivo no es optimizar manualmente, sino medir continuamente el comportamiento del proyecto.

---

## Arquitectura

```text
foundation/performance

performance/
├── PerformanceEngine.java
├── PerformanceMonitor.java
├── PerformanceBudget.java
├── TickBudgetManager.java
├── MemoryMonitor.java
├── CpuMonitor.java
├── ThreadMonitor.java
├── PerformanceSnapshot.java
├── BenchmarkScheduler.java
├── BenchmarkRegistry.java
├── PerformanceAlert.java
└── PerformanceHistory.java
```

Todo el rendimiento del proyecto pasa por este sistema.

---

## Métricas oficiales

Cada módulo debe registrar:

* Tiempo promedio por tick.
* Tiempo máximo por tick.
* Uso de CPU.
* Uso de RAM.
* Uso de hilos.
* Latencia.
* Número de eventos.
* Número de NPC activos.
* Cola de IA.
* Voice Core.
* Scheduler.

---

## Performance Budget

Cada módulo posee un presupuesto máximo.

| Módulo     | Presupuesto                             |
| ---------- | --------------------------------------- |
| Brain      | Tiempo configurable por tick.           |
| Voice Core | Procesamiento fuera del hilo principal. |
| AI Queue   | Latencia máxima configurable.           |
| EventBus   | Tiempo máximo por evento.               |
| Scheduler  | Tiempo máximo por ciclo.                |
| Runtime    | Actualización controlada.               |

Si un módulo excede su presupuesto:

* registrar alerta.
* reducir tareas secundarias.
* generar reporte.

Nunca bloquear Minecraft.

---

# CAPÍTULO 74 — CPU MONITOR

Crear un monitor permanente.

## Responsabilidades

* CPU global.
* CPU por módulo.
* CPU por hilo.
* CPU por NPC.
* CPU por tarea.

Registrar históricos.

---

## Alertas

Generar alertas cuando:

* CPU supera umbral configurado.
* Un hilo consume demasiado tiempo.
* Una tarea permanece demasiado tiempo ejecutándose.

---

# CAPÍTULO 75 — MEMORY MONITOR

## Objetivo

Detectar pérdidas de memoria antes de que ocurran.

### Registrar

* Heap utilizado.
* Heap libre.
* Objetos Runtime.
* Cachés.
* Voice Cache.
* Event Cache.
* Queue Cache.
* Perception Cache.
* Memory Cache.

---

## Leak Detector

Buscar referencias persistentes.

Generar advertencias.

Crear snapshot antes y después de pruebas.

Comparar diferencias.

---

# CAPÍTULO 76 — THREAD MONITOR

Monitor especializado.

### Debe registrar

* Render Thread.
* Client Thread.
* Server Thread.
* Worker Pools.
* Voice Threads.
* Scheduler Threads.
* AI Threads.

---

## Thread Health

Estados:

HEALTHY

BUSY

BLOCKED

WAITING

DEADLOCK_RISK

FAILED

---

# CAPÍTULO 77 — TPS & TICK ANALYZER

Medir el impacto real sobre Minecraft.

### Registrar

* TPS.
* Tick Time.
* Tick Delay.
* Tick Budget.
* Tick Drops.

---

## Tick Alerts

Generar alertas cuando SamuraiAI provoque pérdida de TPS.

---

# CAPÍTULO 78 — BENCHMARK ENGINE

## Filosofía

Benchmark no significa prueba.

Benchmark significa medir capacidad.

---

## BenchmarkRegistry

Registrar benchmarks.

### Categorías

Voice.

Brain.

Queue.

Runtime.

Lifecycle.

Scheduler.

EventBus.

Memory.

Stress.

---

## Ejecución

Cada benchmark produce:

* tiempo.
* memoria.
* CPU.
* hilos.
* resultado.

---

# CAPÍTULO 79 — BENCHMARK SUITES

Crear suites oficiales.

### Voice Benchmark

Medir:

* inicio.
* carga.
* reconocimiento.
* cambio de idioma.
* cancelación.

---

### Brain Benchmark

Medir:

* decisión.
* cambio de Goal.
* procesamiento.

---

### Queue Benchmark

Medir miles de solicitudes.

FIFO.

Timeout.

Cancelaciones.

---

### Runtime Benchmark

Crear cientos de Runtime.

Activar.

Dormir.

Eliminar.

---

# CAPÍTULO 80 — HISTORICAL PERFORMANCE DATABASE

Crear un historial permanente.

Guardar benchmarks anteriores.

Comparar versiones.

Detectar regresiones.

---

## Archivos generados

performance_history.json

benchmark_history.json

performance_summary.md

---

# CAPÍTULO 81 — COMPATIBILITY CERTIFICATION SUITE

Ejecutar compatibilidad completa.

## Escenarios obligatorios

### Cliente Forge

Minecraft inicia correctamente.

Voice disponible.

GUI disponible.

Renderer estable.

---

### Servidor dedicado

Sin GUI.

Sin Voice.

Sin clases cliente.

Sin crashes.

---

### Singleplayer

Cliente + servidor integrado.

Todo funcional.

---

### Multijugador Forge

NPC Runtime.

Eventos.

Queue.

Lifecycle.

---

### CustomNPCs

Instalado.

Ausente.

Versión compatible.

Versión futura.

---

### Voice Core

Micrófono.

Modelo.

Cambio dinámico.

Offline.

Online.

---

### Ollama

Disponible.

No disponible.

Modelo ausente.

Timeout.

---

# CAPÍTULO 82 — REGRESSION CERTIFICATION

Todos los bugs corregidos se prueban nuevamente.

## Casos obligatorios

* Crash de CustomNPCs en servidor.
* Voice bloqueando Render Thread.
* NPC fantasma.
* Queue duplicada.
* Event Loop.
* Runtime inválido.
* Config Migration.

Ningún bug puede reaparecer.

---

# CAPÍTULO 83 — RELEASE PIPELINE

Crear el pipeline oficial de liberación.

## Etapas

Audit.

Testing.

Compatibility.

Benchmark.

Certification.

Packaging.

Documentation.

Release Candidate.

---

## ReleaseCandidateBuilder

Generar builds certificadas.

Nunca generar builds sin certificación.

---

## Release Metadata

Incluir automáticamente:

Versión.

Fecha.

Git Commit (si existe).

Forge.

Java.

Estado Foundation.

---

# CAPÍTULO 84 — BUILD VALIDATION ENGINE

Validar el JAR final.

## Verificaciones

* mods.toml.
* resources.
* manifest.
* assets.
* hashes.
* clases duplicadas.
* clases cliente.
* clases servidor.
* tamaño del JAR.

---

## Packaging Report

Generar:

BUILD_REPORT.md

---

# CAPÍTULO 85 — FOUNDATION CERTIFICATE ENGINE

Este es el corazón de la certificación.

## FoundationCertificateEngine

Responsabilidades.

Leer auditorías.

Leer pruebas.

Leer benchmarks.

Leer compatibilidad.

Generar certificado.

---

## Estados posibles

BLOCKED

FAILED

PARTIAL

READY

Solo READY desbloquea la siguiente fase.

---

## FoundationValidator

Debe comprobar más de 150 verificaciones finales.

### Foundation

Auditoría completa.

Bootstrap.

Resources.

Config.

### Runtime

Lifecycle.

Memory.

Cleanup.

### Voice

READY.

Modelos.

Micrófono.

### Queue

FIFO.

Timeout.

Cancellation.

### EventBus

Registry.

Dispatcher.

Prioridades.

Loops.

### Scheduler

Budget.

Threads.

### Compatibilidad

Cliente.

Servidor.

CustomNPCs.

Ollama.

Voice.

### Performance

CPU.

RAM.

TPS.

Threads.

Benchmarks.

---

# CAPÍTULO 86 — FOUNDATION CERTIFICATE

Generar automáticamente un documento.

## FOUNDATION_CERTIFICATE.md

Debe contener.

### Información general

Versión.

Fecha.

Tiempo de certificación.

Forge.

Java.

---

### Resultado

READY.

PARTIAL.

FAILED.

BLOCKED.

---

### Tabla de módulos

| Módulo           | Estado |
| ---------------- | ------ |
| Foundation Audit | PASS   |
| Bootstrap        | PASS   |
| Voice Core       | PASS   |
| Runtime          | PASS   |
| Lifecycle        | PASS   |
| EventBus         | PASS   |
| Queue            | PASS   |
| Scheduler        | PASS   |
| Diagnostics      | PASS   |
| Compatibility    | PASS   |
| Benchmarks       | PASS   |
| Documentation    | PASS   |

---

### Errores

Lista completa.

---

### Warnings

Lista completa.

---

### Recomendaciones

Automáticas.

---

# CAPÍTULO 87 — FOUNDATION STATUS JSON

Crear archivo utilizado por todas las fases futuras.

```json
{
  "foundation":"READY",
  "version":"1.7",
  "audit":"PASS",
  "compatibility":"PASS",
  "voice":"PASS",
  "runtime":"PASS",
  "benchmarks":"PASS"
}
```

Las siguientes fases deben leer este archivo.

---

# CAPÍTULO 88 — RELEASE CHECKLIST

Codex debe completar automáticamente.

### Arquitectura

* [ ] Auditoría aprobada.
* [ ] Bootstrap aprobado.
* [ ] Cliente aprobado.
* [ ] Servidor aprobado.

### Voice

* [ ] Bootstrap.
* [ ] Model Manager.
* [ ] Download Manager.
* [ ] Thread Safety.

### Runtime

* [ ] Lifecycle.
* [ ] Cleanup.
* [ ] Hibernation.

### EventBus

* [ ] Registry.
* [ ] Dispatcher.
* [ ] Diagnostics.

### Testing

* [ ] JUnit.
* [ ] GameTests.
* [ ] Integration.
* [ ] Regression.
* [ ] Stress.

### Compatibilidad

* [ ] Forge Cliente.
* [ ] Forge Servidor.
* [ ] CustomNPCs.
* [ ] Voice.
* [ ] Ollama.

### Performance

* [ ] CPU.
* [ ] RAM.
* [ ] TPS.
* [ ] Threads.
* [ ] Queue.
* [ ] Runtime.

---

# CAPÍTULO 89 — DOCUMENTACIÓN AUTOMÁTICA

Crear carpeta.

docs/phase1_7/certification/

Archivos obligatorios.

FOUNDATION_CERTIFICATE.md

FOUNDATION_STATUS.md

BENCHMARK_ENGINE.md

PERFORMANCE_SUITE.md

PERFORMANCE_HISTORY.md

BUILD_PIPELINE.md

BUILD_VALIDATION.md

COMPATIBILITY_CERTIFICATION.md

RELEASE_PIPELINE.md

QA_CERTIFICATION.md

FOUNDATION_VALIDATOR.md

FINAL_CHANGELOG.md

Cada documento debe describir arquitectura, métricas, pruebas y resultados.

---

# CAPÍTULO 90 — CRITERIOS PARA DESBLOQUEAR LA FASE 2

La Fase 2 **Behavior Engine** solo puede comenzar cuando todas estas condiciones sean verdaderas.

### Requisitos obligatorios

* Foundation Audit = PASS.
* Client/Server Separation = PASS.
* CustomNPC Compatibility = PASS.
* Voice Core = READY.
* Runtime = HEALTHY.
* EventBus = HEALTHY.
* AI Queue = PASS.
* Scheduler = PASS.
* Thread Audit = PASS.
* Compatibility Suite = PASS.
* Regression Suite = PASS.
* Performance Suite = PASS.
* Foundation Certificate = READY.

Si cualquiera falla.

Behavior Engine queda bloqueado.

Codex debe detener la implementación automáticamente.

---

# EPÍLOGO — FOUNDATION COMPLETE

La Fase 1.7 representa la certificación oficial del núcleo de SamuraiAI.

A partir de este momento el proyecto deja de ser un prototipo y pasa a convertirse en una plataforma modular de inteligencia artificial para Minecraft.

Todo el núcleo queda protegido por auditorías, pruebas, benchmarks, diagnósticos y un certificado automático que garantiza estabilidad antes de permitir nuevas funcionalidades.

**Foundation Complete** establece el estándar de ingeniería que deberán seguir todas las fases futuras del proyecto, desde Behavior Engine hasta Memoria Persistente, Combate, Aprendizaje y la versión final de SamuraiAI.

---

# =========================================================
# RESULTADO ESPERADO GLOBAL — FASE 1.7 FOUNDATION COMPLETE (PARTES 1, 2, 3, 4A Y 4B)
# =========================================================

Con la integración de las cinco partes, SamuraiAI queda transformado, de extremo a extremo, en una plataforma de desarrollo estable, verificable y certificada:

- **Parte 1** entrega el **Foundation Audit Engine**, la separación estricta **cliente/servidor**, el **Module Loader**, el **Resource Manager** y el sistema de **diagnóstico de arranque**, estableciendo la base arquitectónica sobre la que todo lo demás se apoya.
- **Parte 2** cierra por completo el **Voice Core** (bootstrap, instalación, model manager, download manager, configuración y pipeline de audio), introduce el **AsyncEngine** global, corrige los problemas de concurrencia detectados por Codex y profesionaliza la **AI Request Queue** y el **Task Scheduler**.
- **Parte 3** entrega el **Lifecycle definitivo de los NPC** (eliminando los "NPC fantasma"), el **Runtime Manager** con reparación automática, el **EventBus 2.0** con su catálogo completo de eventos, el sistema de **diagnóstico y presupuesto de rendimiento**, y el **Developer Toolkit** completo (consola de depuración, overlay, inspectores y snapshots).
- **Parte 4A** construye el **Testing Framework 2.0**: descubrimiento automático de pruebas, suites JUnit y GameTest, pruebas de integración, regresión y estrés, y las matrices completas de **compatibilidad** (Cliente, Servidor, CustomNPCs, Voice, Ollama), cerrando con el **QA Automation Framework**.
- **Parte 4B** culmina el proceso con la **Performance Suite 2.0** (CPU, memoria, hilos, TPS), el **Benchmark Engine**, la **base de datos histórica de rendimiento**, el **Release Pipeline** con validación de build, y finalmente el **Foundation Certificate Engine**, que emite el certificado oficial (`FOUNDATION_CERTIFICATE.md` y `FOUNDATION_STATUS.json`) que determina si el núcleo está **READY** para desbloquear la Fase 2 — Behavior Engine.

Al completarse las cinco partes: ningún modelo de voz bloquea el hilo principal; ninguna tarea asíncrona modifica Minecraft desde el hilo incorrecto; ningún NPC puede quedar huérfano o fantasma; todos los eventos del proyecto están tipados y catalogados; cada módulo cuenta con pruebas unitarias, de integración, de regresión, de estrés y de compatibilidad; y el rendimiento del proyecto está medido, presupuestado y comparado históricamente. Solo cuando **todos** estos requisitos alcanzan el estado **PASS/READY**, el **Foundation Certificate** puede emitirse como **READY**, y únicamente entonces Codex puede comenzar la Fase 2: **Behavior Engine**.
